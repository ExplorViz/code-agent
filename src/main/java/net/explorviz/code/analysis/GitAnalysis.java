package net.explorviz.code.analysis;

import com.github.javaparser.utils.Pair;
import io.quarkus.runtime.StartupEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.code.analysis.exceptions.MalformedPathException;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.git.GitRepositoryLoader;
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
  private String sourceDirectory;

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

  // @GrpcClient("structureevent")
  // /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS

  private void analyzeAndSendRepo(boolean onlyLast)
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD
    // steps:
    // open or download repository                          - Done
    // get remote state of the analyzed data                - @see GrpcHandler
    // loop for missing commits                             - Done
    //  - find difference between last and "current" commit - Done
    //  - analyze differences                               - Done
    //  - send data chunk                                   - TODO
    try (Repository repository = this.gitRepositoryLoader.getGitRepository()) {

      final String branch = GitRepositoryLoader.getCurrentBranch(repository);

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
          resetSourceDirectory();

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

  private void resetSourceDirectory() {
    this.sourceDirectory = null;
  }

  private String getSourceDirectory() throws MalformedPathException, NotFoundException {
    if (this.sourceDirectory != null) {
      return this.sourceDirectory;
    }
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
      final String[] arr = sourceDir.split("[*\\\\/]");
      final List<String> traverseFolders = new ArrayList<>(Arrays.asList(arr));
      final String dir = findFolder(gitRepositoryLoader.getCurrentRepositoryPath(),
          traverseFolders);
      if (dir.isEmpty()) {
        throw new NotFoundException("directory was not found");
      }
      this.sourceDirectory = new File(dir).getAbsolutePath();

    } else {
      this.sourceDirectory = Path.of(gitRepositoryLoader.getCurrentRepositoryPath(), sourceDir)
          .toString();
    }
    return this.sourceDirectory;
  }

  private static String findFolder(String currentPath, List<String> traverseFolders) {

    // the current path is the folder we searched for, as the traverse folders are empty
    if (traverseFolders.isEmpty()) {
      return currentPath;
    }
    // get all directories in the current directory, so we can search for the right one
    String[] directories = new File(currentPath).list(
        (current, name) -> new File(current, name).isDirectory());
    // if this folder is empty, throw an exception. we only get here if the traverse folder
    // hierarchy is not right, or we got here through a wildcard operator
    if (directories == null) {
      return "";
    }
    // if the next traverse folder is found in the list, search there
    if (Arrays.stream(directories)
        .anyMatch(Predicate.isEqual(traverseFolders.get(0)))) {
      String folderName = traverseFolders.get(0);
      traverseFolders.remove(0);
      return findFolder(currentPath + File.separator + folderName, traverseFolders);
    }
    // this is a wildcard, perform depth-first search
    if (traverseFolders.get(0).isEmpty()) {
      // maybe the wildcard is there, but we are already in the right directory
      if (Arrays.stream(directories)
          .anyMatch(Predicate.isEqual(traverseFolders.get(1)))) {
        traverseFolders.remove(0);
        String folderName = traverseFolders.get(0);
        traverseFolders.remove(0);
        return findFolder(currentPath + File.separator + folderName, traverseFolders);
      }
      for (String directory : directories) {
        List<String> folders = traverseFolders.stream().skip(1).collect(Collectors.toList());
        // search in the next level as the folder is there
        String path = findFolder(currentPath + File.separator + directory, folders);
        if (!path.isEmpty()) {
          return path;
        }
        // search the next level with wildcard
        path = findFolder(currentPath + File.separator + directory, traverseFolders);
        if (!path.isEmpty()) {
          return path;
        }
      }
    }
    // folder was not found
    return "";
  }

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, GitAPIException, PropertyNotDefinedException,
      NotFoundException {
    // TODO: delete, but currently needed for testing
    if (repoPathProperty.isEmpty()) {
      return;
    }
    this.analyzeAndSendRepo(false);
    // this.analyzeAndSendRepo(true);
  }

}
