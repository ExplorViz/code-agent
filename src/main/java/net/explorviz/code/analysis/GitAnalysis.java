package net.explorviz.code.analysis;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.Collection;
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
import net.explorviz.code.analysis.git.GitMetricCollector;
import net.explorviz.code.analysis.git.GitRepositoryHandler;
import net.explorviz.code.analysis.handler.CommitReportHandler;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.parser.JavaParserService;
import net.explorviz.code.analysis.types.FileDescriptor;
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

  @ConfigProperty(name = "explorviz.gitanalysis.remote.url")
  /* default */ Optional<String> repoRemoteUrlProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.source-directory")
  /* default */ Optional<String> sourceDirectoryProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.restrict-analysis-to-folders")
  /* default */ Optional<String> restrictAnalysisToFoldersProperty;  // NOCS NOPMD

  @ConfigProperty(name = "explorviz.gitanalysis.fetch-remote-data", defaultValue = "true")
  /* default */ boolean fetchRemoteDataProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.send-to-remote", defaultValue = "true")
  /* default */ boolean sendToRemoteProperty;  // NOCS

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

  @Inject
  GrpcExporter grpcExporter;


  private void analyzeAndSendRepo(final DataExporter exporter) // NOCS NOPMD
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD

    try (Repository repository = this.gitRepositoryHandler.getGitRepository()) {

      final String branch = repository.getFullBranch();

      // get fetch data from remote
      Optional<String> startCommit = findStartCommit(exporter, branch);
      Optional<String> endCommit = fetchRemoteDataProperty ? Optional.empty() : endCommitProperty;

      checkIfCommitsAreReachable(startCommit, endCommit, branch);

      try (RevWalk revWalk = new RevWalk(repository)) {
        prepareRevWalk(repository, revWalk, branch);

        int commitCount = 0;
        RevCommit lastCheckedCommit = null;
        boolean inAnalysisRange = startCommit.isEmpty() || "".equals(startCommit.get());

        for (final RevCommit commit : revWalk) {

          if (!inAnalysisRange) {
            if (commit.name().equals(startCommit.get())) {
              inAnalysisRange = true;
              if (fetchRemoteDataProperty) {
                lastCheckedCommit = commit;
                continue;
              }
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
            createCommitReport(repository, commit, lastCheckedCommit, exporter, branch);

            commitCount++;
            lastCheckedCommit = commit;
            if (endCommit.isPresent() && commit.name().equals(endCommit.get())) {
              break;
            }
            continue;
          }

          commitAnalysis(repository, commit, lastCheckedCommit, descriptorList, exporter, branch);

          commitCount++;
          lastCheckedCommit = commit;
          // break if endCommit is reached, if endCommit is empty, run for all commits
          if (endCommit.isPresent() && commit.name().equals(endCommit.get())) {
            break;
          }
        }
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Analyzed {} commits", commitCount);
        }
      }
      // checkout the branch, so not a single commit is checked out after the run
      Git.wrap(repository).checkout().setName(branch).call();
    }
  }

  private void checkIfCommitsAreReachable(Optional<String> startCommit, Optional<String> endCommit,
                                          String branch) throws NotFoundException {
    if (this.gitRepositoryHandler.isUnreachableCommit(startCommit, branch)) {
      throw new NotFoundException(toErrorText("start", startCommit.orElse(""), branch));
    } else if (this.gitRepositoryHandler.isUnreachableCommit(endCommit, branch)) {
      throw new NotFoundException(toErrorText("end", endCommit.orElse(""), branch));
    }
  }

  private Optional<String> findStartCommit(DataExporter exporter, String branch) {
    if (fetchRemoteDataProperty) {
      StateData remoteState = exporter.requestStateData(branch);
      if (remoteState.getCommitID().isEmpty() || remoteState.getCommitID().isBlank()) {
        return Optional.empty();
      } else {
        return Optional.of(remoteState.getCommitID());
      }
    } else {
      // happens if value is set by GitLab CI and the "previous latest commit" is unavailable
      if (startCommitProperty.isPresent() && "0000000000000000000000000000000000000000".equals(
          startCommitProperty.get())) {
        return Optional.empty();
      }
      return startCommitProperty;
    }
  }

  private void prepareRevWalk(Repository repository, RevWalk revWalk, String branch)
      throws IOException {
    revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
    revWalk.sort(RevSort.REVERSE, true);

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("analyzing branch " + branch);
    }
    // get a list of all known heads, tags, remotes, ...
    final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();
    for (final Ref ref : allRefs) {
      if (ref.getName().equals(branch)) {
        revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        break;
      }
    }
  }

  private void commitAnalysis(final Repository repository, final RevCommit commit,
                              final RevCommit lastCommit,
                              final List<FileDescriptor> descriptorList,
                              final DataExporter exporter, final String branchName)
      throws GitAPIException, NotFoundException, IOException {
    DirectoryFinder.resetDirectory(sourceDirectoryProperty.orElse(""));

    // final Date commitDate = commit.getAuthorIdent().getWhen();
    Git.wrap(repository).checkout().setName(commit.getName()).call();

    javaParserService.reset(DirectoryFinder.getDirectory(
        List.of(sourceDirectoryProperty.orElse("").split(",")),
        GitRepositoryHandler.getCurrentRepositoryPath()));
    GitMetricCollector.resetAuthor();

    for (final FileDescriptor fileDescriptor : descriptorList) {
      final FileDataHandler fileDataHandler = fileAnalysis(repository, fileDescriptor,
          javaParserService,
          commit.getName());
      if (fileDataHandler != null) {
        GitMetricCollector.addCommitGitMetrics(fileDataHandler, commit);
        exporter.sendFileData(fileDataHandler.getProtoBufObject());
      }
    }
    createCommitReport(repository, commit, lastCommit, exporter, branchName);

  }

  private void createCommitReport(final Repository repository, final RevCommit commit,
                                  final RevCommit lastCommit, final DataExporter exporter,
                                  final String branchName) throws NotFoundException, IOException {
    if (lastCommit == null) {
      commitReportHandler.init(commit.getId().getName(), null, branchName);
    } else {
      commitReportHandler.init(commit.getId().getName(), lastCommit.getId().getName(), branchName);
    }
    List<FileDescriptor> files = gitRepositoryHandler.listFilesInCommit(repository, commit,
        restrictAnalysisToFoldersProperty.orElse(""));
    commitReportHandler.add(files);
    exporter.sendCommitReport(commitReportHandler.getCommitReport());
  }

  private FileDataHandler fileAnalysis(final Repository repository, final FileDescriptor file,
                                       final JavaParserService parser, final String commitSHA)
      throws IOException {
    final String fileContent = GitRepositoryHandler.getContent(file.objectId, repository);
    try {
      FileDataHandler fileDataHandler = parser.parseFileContent(fileContent, file.fileName,
          calculateMetricsProperty, commitSHA); // NOPMD
      GitMetricCollector.addFileGitMetrics(fileDataHandler, file);
      return fileDataHandler;

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

    if (repoPathProperty.isEmpty() && repoRemoteUrlProperty.isEmpty()) {
      return;
    }
    DataExporter exporter;
    if (sendToRemoteProperty) {
      exporter = grpcExporter;
    } else {
      exporter = new JsonExporter();
    }
    analyzeAndSendRepo(exporter);
    Quarkus.asyncExit();
  }


  // only done because checkstyle does not like the duplication of literals
  private static String toErrorText(final String position, final String commitId,
                                    final String branchName) {
    return "The given " + position + " commit <" + commitId
        + "> was not found in the current branch <" + branchName + ">";
  }

}
