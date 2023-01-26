package net.explorviz.code.analysis;

import com.github.javaparser.utils.Pair;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.code.analysis.exceptions.MalformedPathException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.git.GitRepositoryLoader;
import net.explorviz.code.analysis.parser.JavaParserService;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StructureEventServiceGrpc;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
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

  @ConfigProperty(name = "explorviz.gitanalysis.full-analysis")
  /* default */ boolean fullAnalysisProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.calculate-metrics")
  /* default */ boolean calculateMetricsProperty;  // NOCS

  @Inject
  /* package */ GitRepositoryLoader gitRepositoryLoader; // NOCS

  @GrpcClient("structureevent")
  /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS

  private void analyzeAndSendRepo(boolean onlyLast)
      throws IOException, GitAPIException, PropertyNotDefinedException { // NOPMD
    // steps:
    // open or download repository                          - Done
    // get remote state of the analyzed data                - @see GrpcHandler
    // loop for missing commits                             - Done
    //  - find difference between last and "current" commit - Done
    //  - analyze differences                               - Done
    //  - send data chunk                                   - TODO
    try (Repository repository = this.gitRepositoryLoader.getGitRepository()) {

      final String branch = GitRepositoryLoader.getCurrentBranch(repository);
      getSourceDirectory();

      // get a list of all known heads, tags, remotes, ...
      final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();
      // a RevWalk allows to walk over commits based on some filtering that is defined

      try (RevWalk revWalk = new RevWalk(repository)) {

        // sort the commits in ascending order by the commit time (the oldest first)
        revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
        if (!onlyLast) {
          revWalk.sort(RevSort.REVERSE, true);
        }
        LOGGER.info("analyzing branch " + branch);
        for (final Ref ref : allRefs) {
          // find the branch we are interested in
          if (ref.getName().equals(branch)) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            break;
          }
        }


        int count = 0;
        RevCommit old = null;
        for (final RevCommit commit : revWalk) {
          // LOGGER.info("{} : {}", count, commit.toString());
          List<Pair<ObjectId, String>> objectIdList = gitRepositoryLoader.listDiff(repository,
              Optional.ofNullable(old),
              commit);

          if (objectIdList.isEmpty()) {
            LOGGER.info("Skip {}", commit.name());
            count++;
            old = commit;
            continue;
          }

          final Date commitDate = commit.getAuthorIdent().getWhen();
          LOGGER.info("Analyze {}", commitDate);
          Git.wrap(repository).checkout().setName(commit.getName()).call();
          JavaParserService javaParserService = new JavaParserService(getSourceDirectory());


          for (Pair<ObjectId, String> pair : objectIdList) {
            final String fileContent = GitRepositoryLoader.getContent(pair.a, repository);
            LOGGER.info("analyze: {}", pair.b);
            try {
              FileData fileData = javaParserService.parseFileContent(fileContent, pair.b)
                  .getProtoBufObject();
            } catch (NoSuchElementException | NoSuchFieldError e) {
              LOGGER.warn(e.toString());
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

          count++;
          old = commit;
          if (onlyLast) {
            return;
          }
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Analyzed {} commits", count);
        }
      }
    }
  }

  private String getSourceDirectory() throws MalformedPathException {
    // sourceDirectoryProperty.orElse("");
    // folderToAnalyzeProperty.orElse("");
    String sourceDir = sourceDirectoryProperty.orElse("");
    // handle the wildcard
    if (sourceDir.contains("*")) {
      if (sourceDir.matches("\\*[/\\\\]?$")) {
        throw new MalformedPathException(
            "Wildcard character can not be the last, search would not terminate! Given -> "
                + sourceDir);
      }
      if (sourceDir.matches("\\\\\\\\|//")) {
        sourceDir = sourceDir.replaceAll("\\\\", "\\").replaceAll("//", "/");
        LOGGER.warn("found double file separator, replaced input with -> {}", sourceDir);
      }
      String[] parts = sourceDir.split("\\*");
      //   TODO search folder structure for matching folder and set the sourceDir
    }
    return Path.of(gitRepositoryLoader.getCurrentRepositoryPath(), sourceDir).toString();
  }

  public void run() throws GitAPIException, IOException, PropertyNotDefinedException {
    this.analyzeAndSendRepo(true);
  }


  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, NoHeadException, GitAPIException, PropertyNotDefinedException {
    // TODO: delete, but currently needed for testing
    if (repoPathProperty.isEmpty()) {
      return;
    }
    this.analyzeAndSendRepo(false);
    // this.analyzeAndSendRepo(true);
  }

}
