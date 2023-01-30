package net.explorviz.code.analysis;

import com.github.javaparser.utils.Pair;
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
import net.explorviz.code.analysis.git.DirectoryFinder;
import net.explorviz.code.analysis.git.GitRepositoryHandler;
import net.explorviz.code.analysis.parser.JavaParserService;
import net.explorviz.code.proto.FileData;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
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

  @ConfigProperty(name = "explorviz.gitanalysis.restrict-to-folder")
  /* default */ Optional<String> folderToAnalyzeProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.full-analysis", defaultValue = "false")
  /* default */ boolean fullAnalysisProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.calculate-metrics", defaultValue = "true")
  /* default */ boolean calculateMetricsProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.start-commit-sha1")
  /* default */ Optional<String> startCommitProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.end-commit-sha1")
  /* default */ Optional<String> endCommitProperty;  // NOCS

  @Inject
  /* package */ GitRepositoryHandler gitRepositoryHandler; // NOCS

  // @GrpcClient("structureevent")
  // /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS

  private void analyzeAndSendRepo(String startCommit, String endCommit)// NOCS NOPMD TODO cyclomatic
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD
    // steps:
    // open or download repository                          - Done
    // get remote state of the analyzed data                - @see GrpcHandler
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
        boolean inAnlysisRange = "".equals(startCommit);

        JavaParserService javaParserService;
        for (final RevCommit commit : revWalk) {

          if (!inAnlysisRange) {
            if (commit.name().equals(startCommit)) {
              inAnlysisRange = true;
            } else {
              if (!fullAnalysisProperty) {
                lastCheckedCommit = commit;
              }
              continue;
            }
          }

          // LOGGER.info("{} : {}", count, commit.toString());
          final List<Pair<ObjectId, String>> objectIdList = gitRepositoryHandler.listDiff(
              repository,
              Optional.ofNullable(lastCheckedCommit),
              commit);

          if (objectIdList.isEmpty()) {
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
          DirectoryFinder.resetDirectory(sourceDirectoryProperty.orElse(""));

          final Date commitDate = commit.getAuthorIdent().getWhen();
          LOGGER.info("Analyze {}", commitDate);
          Git.wrap(repository).checkout().setName(commit.getName()).call();
          javaParserService = new JavaParserService(// NOPMD
              DirectoryFinder.getDirectory(sourceDirectoryProperty.orElse("")));


          for (final Pair<ObjectId, String> pair : objectIdList) {
            final String fileContent = GitRepositoryHandler.getContent(pair.a, repository);
            LOGGER.info("analyze: {}", pair.b);
            try {
              FileData fileData = javaParserService.parseFileContent(fileContent, pair.b) // NOPMD
                  .getProtoBufObject();
            } catch (NoSuchElementException | NoSuchFieldError e) {
              if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(e.toString());
              }
            }


            // TODO: enable GRPC again
            // for (int i = 0; i < classes.size(); i++) {
            //   final StructureFileEvent event = classes.get(i);
            //   final StructureFileEvent eventWithTiming = StructureFileEvent.newBuilder(event)
            //       .setEpochMilli(authorIdent.getWhen().getTime()).build();
            //   classes.set(i, eventWithTiming);
            //   // grpcClient.sendStructureFileEvent(event).await().indefinitely();
            //   grpcClient.sendStructureFileEvent(event);
            // }

            // if (LOGGER.isDebugEnabled()) {
            //   LOGGER.debug("Classes names: {}", classes);
            // }
          }

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

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, GitAPIException, PropertyNotDefinedException,
      NotFoundException {
    // TODO: delete, but currently needed for testing
    if (repoPathProperty.isEmpty()) {
      return;
    }
    this.analyzeAndSendRepo(startCommitProperty.orElse(""), endCommitProperty.orElse(""));
    // this.analyzeAndSendRepo("f3a8d244b2d3c52325941d09cdeb1b07b8b37815",
    //     "6580e8b6cfa246422399eb0640ef93c30396115d");
  }

  private static String toErrorText(final String position, final String commitId,
                                    final String branchName) {
    return "The given " + position + " commit <" + commitId
        + "> was not found in the current branch <" + branchName + ">";
  }

}
