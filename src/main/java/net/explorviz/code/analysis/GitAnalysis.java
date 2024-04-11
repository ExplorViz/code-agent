package net.explorviz.code.analysis;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.exceptions.DebugFileWriter;
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
import net.explorviz.code.analysis.handler.FileMetricHandler;
import net.explorviz.code.analysis.parser.JavaParserService;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.analysis.types.Triple;
import net.explorviz.code.analysis.visitor.CyclomaticComplexityVisitor;
import net.explorviz.code.analysis.visitor.FileDataVisitor;
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
public class GitAnalysis { // NOPMD

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

  @ConfigProperty(name = "explorviz.gitanalysis.save-crashed_files")
  /* default */ boolean saveCrashedFilesProperty;  // NOCS

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeTokenProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.application-name")
  /* default */ String applicationNameProperty;  // NOCS

  @Inject
  /* package */ GitRepositoryHandler gitRepositoryHandler; // NOCS

  @Inject
  /* package */ JavaParserService javaParserService; // NOCS

  @Inject
  /* package */ CommitReportHandler commitReportHandler; // NOCS

  @Inject
  /* package */ GrpcExporter grpcExporter; // NOCS

  // only done because checkstyle does not like the duplication of literals
  private static String toErrorText(final String position, final String commitId,
      final String branchName) {
    return "The given " + position + " commit <" + commitId
        + "> was not found in the current branch <" + branchName + ">";
  }

  private void analyzeAndSendRepo(final DataExporter exporter) // NOCS NOPMD
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD

    try (Repository repository = this.gitRepositoryHandler.getGitRepository()) {

      final String branch = repository.getFullBranch();

      // get fetch data from remote
      final Optional<String> startCommit = findStartCommit(exporter, branch);
      final Optional<String> endCommit =
          fetchRemoteDataProperty ? Optional.empty() : endCommitProperty;

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

          final Triple<List<FileDescriptor>, List<FileDescriptor>, List<FileDescriptor>>
              descriptorTriple = gitRepositoryHandler.listDiff(repository,
              Optional.ofNullable(lastCheckedCommit), commit,
              restrictAnalysisToFoldersProperty.orElse(""));

          final List<FileDescriptor> descriptorAddedList = descriptorTriple.getRight(); // NOPMD
          final List<FileDescriptor> descriptorModifiedList = descriptorTriple.getLeft();

          // TODO: delete this line. It was just used for mocking purposes
          // descriptorAddedList = gitRepositoryHandler.listFilesInCommit(repository, commit,
          //     restrictAnalysisToFoldersProperty.orElse(""));

          // DirectoryFinder.resetDirectory(sourceDirectoryProperty.orElse(""));
          // Git.wrap(repository).checkout().setName(commit.getName()).call();

          // javaParserService.reset(DirectoryFinder.getDirectory(List.of(sourceDirectoryProperty
          //     .orElse("").split(",")), GitRepositoryHandler.getCurrentRepositoryPath()));
          // GitMetricCollector.resetAuthor();

          if (descriptorAddedList.isEmpty() && descriptorModifiedList.isEmpty()) {
            createCommitReport(repository, commit, lastCheckedCommit, exporter, branch,
                descriptorTriple, new HashMap<>()); // NOPMD

            commitCount++;
            lastCheckedCommit = commit;
            if (endCommit.isPresent() && commit.name().equals(endCommit.get())) {
              break;
            }
            continue;
          }

          final List<FileDescriptor> descriptorList = new ArrayList<FileDescriptor>(); // NOPMD
          descriptorList.addAll(descriptorAddedList);
          descriptorList.addAll(descriptorModifiedList);

          commitAnalysis(repository, commit, lastCheckedCommit, descriptorList, exporter, branch,
              descriptorTriple);

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

  private void checkIfCommitsAreReachable(final Optional<String> startCommit,
      final Optional<String> endCommit, final String branch)
      throws NotFoundException {
    if (this.gitRepositoryHandler.isUnreachableCommit(startCommit, branch)) {
      throw new NotFoundException(toErrorText("start", startCommit.orElse(""), branch));
    } else if (this.gitRepositoryHandler.isUnreachableCommit(endCommit, branch)) {
      throw new NotFoundException(toErrorText("end", endCommit.orElse(""), branch));
    }
  }

  private Optional<String> findStartCommit(final DataExporter exporter, final String branch) {
    if (fetchRemoteDataProperty) {
      final StateData remoteState = exporter.requestStateData(getUnambiguousUpstreamName(), branch);
      if (remoteState.getCommitID().isEmpty() || remoteState.getCommitID().isBlank()) {
        return Optional.empty();
      } else {
        return Optional.of(remoteState.getCommitID());
      }
    } else {
      if (startCommitProperty.isPresent() && exporter.isInvalidCommitHash(
          startCommitProperty.get())) {
        return Optional.empty();
      }
      return startCommitProperty;
    }
  }

  private void prepareRevWalk(final Repository repository, final RevWalk revWalk,
      final String branch) throws IOException {
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
      final RevCommit lastCommit, final List<FileDescriptor> descriptorList,
      final DataExporter exporter, final String branchName,
      final Triple<List<FileDescriptor>, List<FileDescriptor>,
          List<FileDescriptor>> descriptorTriple)
      throws GitAPIException, NotFoundException, IOException {
    DirectoryFinder.resetDirectory(sourceDirectoryProperty.orElse(""));

    // final Date commitDate = commit.getAuthorIdent().getWhen();
    Git.wrap(repository).checkout().setName(commit.getName()).call();

    javaParserService.reset(
        DirectoryFinder.getDirectory(List.of(sourceDirectoryProperty.orElse("").split(",")),
            GitRepositoryHandler.getCurrentRepositoryPath()));
    GitMetricCollector.resetAuthor();

    final Map<String, FileDataHandler> fileNameToFileDataHandlerMap = new HashMap<>();

    for (final FileDescriptor fileDescriptor : descriptorList) {
      final FileDataHandler fileDataHandler = fileAnalysis(repository, fileDescriptor,
          javaParserService, commit.getName());
      if (fileDataHandler == null) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("Analysis of file " + fileDescriptor.relativePath + " failed.");
        }
      } else {
        GitMetricCollector.addCommitGitMetrics(fileDataHandler, commit);
        fileDataHandler.setLandscapeToken(landscapeTokenProperty);
        fileDataHandler.setApplicationName(applicationNameProperty);
        exporter.sendFileData(fileDataHandler.getProtoBufObject());
        fileNameToFileDataHandlerMap.put(fileDescriptor.relativePath, fileDataHandler);
      }
    }
    createCommitReport(repository, commit, lastCommit, exporter, branchName,
        descriptorTriple, fileNameToFileDataHandlerMap);

  }

  private void createCommitReport(final Repository repository, final RevCommit commit, // NOPMD
      final RevCommit lastCommit, final DataExporter exporter,
      final String branchName,
      final Triple<List<FileDescriptor>, List<FileDescriptor>,
          List<FileDescriptor>> descriptorTriple,
      final Map<String, FileDataHandler> fileNameToFileDataHandlerMap)
      throws NotFoundException, IOException, GitAPIException {
    //final String commitTag = Git.wrap(repository).describe().setTarget(commit.getId()).call();
    if (lastCommit == null) {
      commitReportHandler.init(commit.getId().getName(), null, branchName);
    } else {
      commitReportHandler.init(commit.getId().getName(), lastCommit.getId().getName(), branchName);
    }
    final List<FileDescriptor> files = gitRepositoryHandler.listFilesInCommit(repository, commit,
        restrictAnalysisToFoldersProperty.orElse(""));
    commitReportHandler.add(files);

    for (final FileDescriptor file : files) {
      commitReportHandler.addFileHash(file);
      final FileDataHandler fileDataHandler = fileNameToFileDataHandlerMap.get(file.relativePath);

      if (fileDataHandler != null) { // add metrics
        final FileMetricHandler fileMetricHandler = commitReportHandler
            .getFileMetricHandler(file.relativePath);

        fileMetricHandler.setFileName(file.relativePath);

        // Set loc metric
        final String loc = fileDataHandler
            .getMetricValue(FileDataVisitor.LOC);

        if (loc != null) {
          fileMetricHandler.setLoc(Integer.parseInt(loc));
        }

        // Set number of methods
        fileMetricHandler.setNumberOfMethods(fileDataHandler.getMethodCount());

        // Set cyclomatic complexity
        final String cyclomaticComplexity = fileDataHandler
            .getMetricValue(CyclomaticComplexityVisitor.CYCLOMATIC_COMPLEXITY);

        if (cyclomaticComplexity != null) {
          fileMetricHandler.setCyclomaticComplexity(Integer.parseInt(cyclomaticComplexity));
        }
      }
    }

    final List<FileDescriptor> modifiedFiles = descriptorTriple.getLeft();
    final List<FileDescriptor> deletedFiles = descriptorTriple.getMiddle();
    final List<FileDescriptor> addedFiles = descriptorTriple.getRight();

    for (final FileDescriptor modifiedFile : modifiedFiles) {
      commitReportHandler.addModified(modifiedFile);
    }

    for (final FileDescriptor deletedFile : deletedFiles) {
      commitReportHandler.addDeleted(deletedFile);
    }

    for (final FileDescriptor addedFile : addedFiles) {
      commitReportHandler.addAdded(addedFile);
    }

    final List<Ref> list = Git.wrap(repository).tagList().call();
    final List<String> tags = new ArrayList<>();
    for (final Ref tag : list) {
      if (tag.getObjectId().equals(commit.getId())) {
        tags.add(tag.getName());
      }
    }
    commitReportHandler.addTags(tags);
    commitReportHandler.addToken(landscapeTokenProperty);
    commitReportHandler.addApplicationName(applicationNameProperty);

    exporter.sendCommitReport(commitReportHandler.getCommitReport());
  }

  private FileDataHandler fileAnalysis(final Repository repository, final FileDescriptor file,
      final JavaParserService parser, final String commitSha)
      throws IOException {
    final String fileContent = GitRepositoryHandler.getContent(file.objectId, repository);
    try {
      final FileDataHandler fileDataHandler = parser.parseFileContent(fileContent, file.fileName,
          calculateMetricsProperty, commitSha); // NOPMD
      if (fileDataHandler == null) {
        if (saveCrashedFilesProperty) {
          DebugFileWriter.saveDebugFile("/logs/crashedfiles/", fileContent,
              file.fileName);
        }
      } else {
        GitMetricCollector.addFileGitMetrics(fileDataHandler, file);
      }
      return fileDataHandler;

    } catch (NoSuchElementException | NoSuchFieldError e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(e.toString());
      }
      return null;
    }
  }

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException {

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
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Analysis finished successfully, exiting now.");
    }
    Quarkus.asyncExit();
    // Quarkus.waitForExit();
    // System.exit(-1); // NOPMD

  }

  private String getUnambiguousUpstreamName() {
    if (repoRemoteUrlProperty.isPresent()) {
      // truncate https or anything else before the double slash
      String upstream = repoRemoteUrlProperty.get();
      // delete http(s):// or git@ in the front
      upstream = upstream.replaceFirst("^(https?://|.+@)", "");
      // replace potential .git ending
      upstream = upstream.replaceFirst("\\.git$", "");
      return upstream;
    } else {
      return "";
    }
  }

}
