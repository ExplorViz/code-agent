package net.explorviz.code.analysis;

import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.export.GrpcExporter;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.git.DirectoryFinder;
import net.explorviz.code.analysis.git.GitRepositoryHandler;
import net.explorviz.code.analysis.handler.CommitReportHandler;
import net.explorviz.code.analysis.parser.JavaParserService;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint for this service. Expects a local path to a Git repository folder
 * ("explorviz.repo.folder.path"). Sends the analysis's results to ExplorViz code service.
 */
@ApplicationScoped
public class GitAnalysis {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitAnalysis.class);

  @ConfigProperty(name = "explorviz.gitanalysis.local.storage-path")
  /* default */ Optional<String> repoPathProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.source-directory")
  /* default */ Optional<String> sourceDirectoryProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.restrict-analysis-to-folders")
  /* default */ Optional<String> restrictAnalysisToFoldersProperty;  // NOCS NOPMD

  @ConfigProperty(name = "explorviz.gitanalysis.branch")
  /* default */ Optional<String> repositoryBranchProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.fetch-remote-data", defaultValue = "true")
  /* default */ boolean fetchRemoteDataProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.calculate-metrics", defaultValue = "true")
  /* default */ boolean calculateMetricsProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.start-commit-sha1")
  /* default */ Optional<String> startCommitProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.end-commit-sha1")
  /* default */ Optional<String> endCommitProperty;  // NOCS

  @Inject
  /* package */ GitRepositoryHandler gitRepositoryHandler; // NOCS

  @Inject
  /* package */ JavaParserService javaParserService; // NOCS

  @Inject
  /* package */ CommitReportHandler commitReportHandler; // NOCS


  // TODO change startCommit and endCommit to Optional<String>
  private void analyzeAndSendRepo(final String startCommit,// NOCS NOPMD TODO cyclomatic complexity
                                  final String endCommit,
                                  final DataExporter exporter)
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD
    // steps:
    // open or download repository                          - Done
    // get remote state of the analyzed data                - @see GrpcExporter
    // loop for missing commits                             - Done
    //  - find difference between last and "current" commit - Done
    //  - analyze differences                               - Done
    //  - send data chunk                                   - TODO
    try (Repository repository = this.gitRepositoryHandler.getGitRepository()) {

      final String branch = GitRepositoryHandler.getCurrentBranch(repository);

      if (this.gitRepositoryHandler.isUnreachableCommit(startCommit, branch)) {
        throw new NotFoundException(toErrorText("start", startCommit, branch));
      } else if (this.gitRepositoryHandler.isUnreachableCommit(endCommit, branch)) {
        throw new NotFoundException(toErrorText("end", endCommit, branch));
      }

      // get a list of all known heads, tags, remotes, ...
      final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();
      // a RevWalk allows to walk over commits based on some filtering that is defined

      try (RevWalk revWalk = new RevWalk(repository)) {

        // sort the commits in ascending order by the commit time (the oldest first)
        revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
        revWalk.sort(RevSort.REVERSE, true);

        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("analyzing branch " + branch);
        }

        for (final Ref ref : allRefs) {
          // find the branch we are interested in
          if (ref.getName().equals(branch)) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            break;
          }
        }

        int commitCount = 0;
        RevCommit lastCheckedCommit = null;
        boolean inAnalysisRange = "".equals(startCommit);

        for (final RevCommit commit : revWalk) {

          if (!inAnalysisRange) {
            if (commit.name().equals(startCommit)) {
              inAnalysisRange = true;
            } else {
              if (fetchRemoteDataProperty) {
                lastCheckedCommit = commit;
              }
              continue;
            }
          }

          final List<FileDescriptor> descriptorList = gitRepositoryHandler.listDiff(repository,
              Optional.ofNullable(lastCheckedCommit), commit,
              restrictAnalysisToFoldersProperty.orElse(""));

          if (descriptorList.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
              LOGGER.info("Skip {}", commit.name());
            }
            commitCount++;
            lastCheckedCommit = commit;
            if (commit.name().equals(endCommit)) {
              break;
            }
            continue;
          }

          commitAnalysis(repository, commit, descriptorList, exporter);

          commitCount++;
          lastCheckedCommit = commit;
          // break if endCommit is reached, if endCommit is null, run for all commits
          if (commit.name().equals(endCommit)) {
            break;
          }
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Analyzed {} commits", commitCount);
        }
      }
    }
  }

  private void commitAnalysis(final Repository repository, final RevCommit commit,
                              final List<FileDescriptor> descriptorList,
                              final DataExporter exporter)
      throws GitAPIException, NotFoundException, IOException {
    DirectoryFinder.resetDirectory(sourceDirectoryProperty.orElse(""));
    commitReportHandler.init(commit.getId().getName(),
        GitRepositoryHandler.getCurrentBranch(repository));

    final Date commitDate = commit.getAuthorIdent().getWhen();
    LOGGER.info("Analyze {}", commitDate);
    Git.wrap(repository).checkout().setName(commit.getName()).call();
    // parser = new JavaParserService(// NOPMD
    //     DirectoryFinder.getDirectory(sourceDirectoryProperty.orElse("")));
    javaParserService.reset(DirectoryFinder.getDirectory(
        List.of(sourceDirectoryProperty.orElse("").split(","))));

    for (final FileDescriptor fileDescriptor : descriptorList) {
      final FileData fileData = fileAnalysis(repository, fileDescriptor, javaParserService);
      // TODO Export Alex
      exporter.sendFileData(fileData);
    }
    List<FileDescriptor> files = gitRepositoryHandler.listFilesInCommit(repository, commit,
        restrictAnalysisToFoldersProperty.orElse(""));
    commitReportHandler.add(files);
    exporter.sendCommitReport(commitReportHandler.getCommitReport());
  }

  private FileData fileAnalysis(final Repository repository, final FileDescriptor file,
                                final JavaParserService parser) throws IOException {
    final String fileContent = GitRepositoryHandler.getContent(file.objectId, repository);
    LOGGER.info("analyze: {}", file.fileName);
    try {
      return parser.parseFileContent(fileContent, file.fileName, calculateMetricsProperty) // NOPMD
          .getProtoBufObject();

    } catch (NoSuchElementException | NoSuchFieldError e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(e.toString());
      }
      // TODO Return something more reasonable, null is clearly not good
      return null;
    }
  }

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, GitAPIException, PropertyNotDefinedException,
      NotFoundException {
    // TODO: delete, but currently needed for testing
    if (repoPathProperty.isEmpty()) {
      return;
    }
    DataExporter exporter;
    // check if running local or remote enabled
    // TODO seems unclean to use this property to decide if the analysis runs locally AND if the
    //  state should be checked. Are these always bound together?
    if (fetchRemoteDataProperty) {
      exporter = new GrpcExporter();
    } else {
      exporter = new JsonExporter("");
      // pathToStorageDirectoryProperty.orElseThrow(
      // () -> new PropertyNotDefinedException("pathToStorageDirectoryProperty"));
    }
    // get fetch data from remote
    StateData remoteState = exporter.requestStateData(repositoryBranchProperty.orElse(""));
    if (remoteState.getCommitID().isEmpty()) {
      analyzeAndSendRepo(startCommitProperty.orElse(""), endCommitProperty.orElse(""),
          exporter);
    } else {
      analyzeAndSendRepo(remoteState.getCommitID(), "", exporter);
    }
  }

  // only done because checkstyle does not like the duplication of literals
  private static String toErrorText(final String position, final String commitId,
                                    final String branchName) {
    return "The given " + position + " commit <" + commitId
        + "> was not found in the current branch <" + branchName + ">";
  }

}
