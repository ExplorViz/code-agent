package net.explorviz.code.analysis.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.explorviz.code.analysis.exceptions.DebugFileWriter;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.git.DirectoryFinder;
import net.explorviz.code.analysis.git.GitMetricCollector;
import net.explorviz.code.analysis.git.GitRepositoryHandler;
import net.explorviz.code.analysis.handler.AbstractFileDataHandler;
import net.explorviz.code.analysis.handler.CommitReportHandler;
import net.explorviz.code.analysis.handler.JavaFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrParserService;
import net.explorviz.code.analysis.parser.AntlrPythonParserService;
import net.explorviz.code.analysis.parser.AntlrTypeScriptParserService;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.analysis.types.Triple;
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
 * Service for analyzing Git repositories and extracting code metrics.
 */
@ApplicationScoped
public class AnalysisService { // NOPMD

  private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisService.class);

  @Inject
  /* package */ GitRepositoryHandler gitRepositoryHandler; // NOCS

  @Inject
  /* package */ AntlrParserService antlrParserService; // NOCS (ANTLR-based Java parser)

  @Inject
  /* package */ AntlrTypeScriptParserService tsParserService; // NOCS

  @Inject
  /* package */ AntlrPythonParserService pythonParserService; // NOCS

  @Inject
  /* package */ CommitReportHandler commitReportHandler; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.save-crashed_files")
  /* default */ boolean saveCrashedFilesProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.fetch-remote-data", defaultValue = "true")
  /* default */ boolean fetchRemoteDataProperty; // NOCS

  // only done because checkstyle does not like the duplication of literals
  private static String toErrorText(final String position, final String commitId,
      final String branchName) {
    return "The given " + position + " commit <" + commitId
        + "> was not found in the current branch <" + branchName + ">";
  }

  /**
   * Analyzes a Git repository and sends the results using the provided exporter.
   *
   * @param config   The analysis configuration
   * @param exporter The data exporter to use for sending results
   * @throws IOException                 If an I/O error occurs
   * @throws GitAPIException             If a Git operation fails
   * @throws NotFoundException           If a required resource is not found
   * @throws PropertyNotDefinedException If a required property is not defined
   */
  public void analyzeAndSendRepo(final AnalysisConfig config, final DataExporter exporter) // NOCS
      throws IOException, GitAPIException, NotFoundException, PropertyNotDefinedException { // NOPMD

    try (Repository repository = this.gitRepositoryHandler.getGitRepository(config)) {

      final String branch = repository.getFullBranch();

      // get fetch data from remote
      final Optional<String> startCommit = findStartCommit(config, exporter, branch);
      final Optional<String> endCommit =
          config.isFetchRemoteData() ? Optional.empty() : config.getEndCommit();

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
              if (config.isFetchRemoteData()) {
                lastCheckedCommit = commit;
                continue;
              }
            } else {
              if (config.isFetchRemoteData()) {
                lastCheckedCommit = commit;
              }
              continue;
            }
          }

          LOGGER.atDebug().addArgument(commit.getName()).log("Analyzing commit: {}");

          final Triple<List<FileDescriptor>, List<FileDescriptor>, List<FileDescriptor>>
              descriptorTriple = gitRepositoryHandler
              .listDiff(repository,
                  Optional.ofNullable(lastCheckedCommit), commit,
                  config.getRestrictAnalysisToFolders().orElse(""));

          final List<FileDescriptor> descriptorAddedList = descriptorTriple.getRight(); // NOPMD
          final List<FileDescriptor> descriptorModifiedList = descriptorTriple.getLeft();

          LOGGER.atDebug().addArgument(descriptorAddedList.size())
              .addArgument(descriptorModifiedList.size())
              .log("Files added: {}, files modified: {}");

          if (descriptorAddedList.isEmpty() && descriptorModifiedList.isEmpty()) {
            createCommitReport(config, repository, commit, lastCheckedCommit, exporter, branch,
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

          commitAnalysis(config, repository, commit, lastCheckedCommit, descriptorList, exporter,
              branch, descriptorTriple);

          commitCount++;
          lastCheckedCommit = commit;
          // break if endCommit is reached, if endCommit is empty, run for all commits
          if (endCommit.isPresent() && commit.name().equals(endCommit.get())) {
            break;
          }
        }

        LOGGER.atTrace().addArgument(commitCount).log("Analyzed {} commits");
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

  private Optional<String> findStartCommit(final AnalysisConfig config,
      final DataExporter exporter, final String branch) {
    if (config.isFetchRemoteData()) {
      final StateData remoteState = exporter.requestStateData(
          getUnambiguousUpstreamName(config.getRepoRemoteUrl()), branch,
          config.getLandscapeToken(), config.getApplicationName());
      if (remoteState.getCommitId().isEmpty() || remoteState.getCommitId().isBlank()) {
        return Optional.empty();
      } else {
        return Optional.of(remoteState.getCommitId());
      }
    } else {
      if (config.getStartCommit().isPresent() && exporter.isInvalidCommitHash(
          config.getStartCommit().get())) {
        return Optional.empty();
      }
      return config.getStartCommit();
    }
  }

  private void prepareRevWalk(final Repository repository, final RevWalk revWalk,
      final String branch) throws IOException {
    revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
    revWalk.sort(RevSort.REVERSE, true);

    LOGGER.atTrace().addArgument(branch).log("Analyzing branch: {}");

    // get a list of all known heads, tags, remotes, ...
    final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();
    for (final Ref ref : allRefs) {
      if (ref.getName().equals(branch)) {
        revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        break;
      }
    }
  }

  private void commitAnalysis(final AnalysisConfig config, final Repository repository,
      final RevCommit commit, final RevCommit lastCommit, final List<FileDescriptor> descriptorList,
      final DataExporter exporter, final String branchName,
      final Triple<List<FileDescriptor>, List<FileDescriptor>,
          List<FileDescriptor>> descriptorTriple)
      throws GitAPIException, NotFoundException, IOException {
    DirectoryFinder.resetDirectory(config.getSourceDirectory().orElse(""));

    Git.wrap(repository).checkout().setName(commit.getName()).call();

    antlrParserService.reset();
    GitMetricCollector.resetAuthor();

    final Map<String, AbstractFileDataHandler> fileNameToFileDataHandlerMap = new HashMap<>();

    LOGGER.atTrace().addArgument(descriptorList.toString()).log("Files: {}");

    for (final FileDescriptor fileDescriptor : descriptorList) {
      LOGGER.atInfo()
          .addArgument(fileDescriptor.relativePath)
          .log("📄 Analyzing file: {}");

      final AbstractFileDataHandler fileDataHandler =
          fileAnalysis(config, repository, fileDescriptor,
              commit.getName());

      if (fileDataHandler == null) {
        LOGGER.atError()
            .addArgument(fileDescriptor.relativePath)
            .log("❌ Analysis of file {} failed - handler is NULL");
      } else {
        LOGGER.atInfo()
            .addArgument(fileDescriptor.relativePath)
            .log("✅ Analysis of file {} succeeded - sending to exporter");
        try {
          File file = new File(GitRepositoryHandler.getCurrentRepositoryPath() + "/"
              + fileDescriptor.relativePath);
          fileDataHandler.addMetric(FileDataVisitor.FILE_SIZE, String.valueOf(file.length()));
        } catch (NullPointerException e) {
          LOGGER.error("File size of file " + fileDescriptor.relativePath
              + " could not be analyzed." + e.getMessage());
        }
        // Only add Git metrics for Java files (JavaFileDataHandler type)
        if (fileDataHandler instanceof JavaFileDataHandler) {
          GitMetricCollector.addCommitGitMetrics(fileDataHandler, commit);
        }
        fileDataHandler.setLandscapeToken(config.getLandscapeToken());
        exporter.sendFileData(fileDataHandler.getProtoBufObject());
        fileNameToFileDataHandlerMap.put(fileDescriptor.relativePath, fileDataHandler);
      }
    }
    createCommitReport(config, repository, commit, lastCommit, exporter, branchName,
        descriptorTriple, fileNameToFileDataHandlerMap);

  }

  private void createCommitReport(final AnalysisConfig config, final Repository repository,
      final RevCommit commit, final RevCommit lastCommit, final DataExporter exporter,
      final String branchName,
      final Triple<List<FileDescriptor>, List<FileDescriptor>,
          List<FileDescriptor>> descriptorTriple,
      final Map<String, AbstractFileDataHandler> fileNameToFileDataHandlerMap)
      throws NotFoundException, IOException, GitAPIException {
    if (lastCommit == null) {
      commitReportHandler.init(commit.getId().getName(), null, branchName);
    } else {
      commitReportHandler.init(commit.getId().getName(), lastCommit.getId().getName(), branchName);
    }
    final List<FileDescriptor> files = gitRepositoryHandler.listFilesInCommit(repository, commit,
        config.getRestrictAnalysisToFolders().orElse(""));
    commitReportHandler.add(files);

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
    commitReportHandler.addToken(config.getLandscapeToken());
    commitReportHandler.setRepositoryName(config.getApplicationName());

    exporter.sendCommitReport(commitReportHandler.getCommitData());
  }

  /**
   * Analyzes a file and returns the appropriate handler based on file extension. Routes .java files
   * to JavaParserService and .ts/.js files to TypeScriptParserService.
   *
   * @param config     the analysis configuration
   * @param repository the git repository
   * @param file       the file descriptor
   * @param commitSha  the commit SHA
   * @return the file data handler (Java or TypeScript specific)
   * @throws IOException if file content cannot be read
   */
  private AbstractFileDataHandler fileAnalysis(final AnalysisConfig config,
      final Repository repository, final FileDescriptor file, final String commitSha)
      throws IOException {
    final String fileContent = GitRepositoryHandler.getContent(file.objectId, repository);
    final String fileName = file.fileName.toLowerCase();

    try {
      AbstractFileDataHandler fileDataHandler = null;

      // Route to appropriate parser based on file extension
      if (fileName.endsWith(".ts") || fileName.endsWith(".tsx")
          || fileName.endsWith(".js") || fileName.endsWith(".jsx")) {
        // TypeScript/JavaScript file
        LOGGER.atInfo()
            .addArgument(file.fileName)
            .addArgument(fileContent.length())
            .log("Parsing TypeScript/JavaScript file: {} (size: {} bytes)");

        fileDataHandler =
            tsParserService.parseFileContent(fileContent, file.relativePath, commitSha);

        if (fileDataHandler != null) {
          // Add git metrics to the TypeScript/JavaScript file handler
          GitMetricCollector.addFileGitMetrics(fileDataHandler, file);
          LOGGER.atInfo()
              .addArgument(file.fileName)
              .log("✅ Successfully parsed TypeScript/JavaScript file: {}");
        } else {
          LOGGER.atError()
              .addArgument(file.fileName)
              .log("❌ TypeScript parser returned NULL for file: {}");
        }
      } else if (fileName.endsWith(".java")) {
        // Java file - using ANTLR parser
        LOGGER.atInfo()
            .addArgument(file.fileName)
            .addArgument(fileContent.length())
            .log("Parsing Java file with ANTLR: {} (size: {} bytes)");

        fileDataHandler =
            antlrParserService.parseFileContent(fileContent, file.relativePath, commitSha);

        if (fileDataHandler != null) {
          // Add git metrics to the Java file handler
          GitMetricCollector.addFileGitMetrics(fileDataHandler, file);
          LOGGER.atInfo()
              .addArgument(file.fileName)
              .log("✅ Successfully parsed Java file with ANTLR: {}");
        } else {
          LOGGER.atError()
              .addArgument(file.fileName)
              .log("❌ ANTLR Java parser returned NULL for file: {}");
        }
      } else if (fileName.endsWith(".py")) {
        // Python file - using ANTLR parser
        LOGGER.atInfo()
            .addArgument(file.fileName)
            .addArgument(fileContent.length())
            .log("Parsing Python file with ANTLR: {} (size: {} bytes)");

        fileDataHandler =
            pythonParserService.parseFileContent(fileContent, file.relativePath, commitSha);

        if (fileDataHandler != null) {
          // Add git metrics to the Python file handler
          GitMetricCollector.addFileGitMetrics(fileDataHandler, file);
          LOGGER.atInfo()
              .addArgument(file.fileName)
              .log("✅ Successfully parsed Python file with ANTLR: {}");
        } else {
          LOGGER.atError()
              .addArgument(file.fileName)
              .log("❌ ANTLR Python parser returned NULL for file: {}");
        }
      } else {
        LOGGER.atWarn()
            .addArgument(file.fileName)
            .log("Unsupported file type: {}");
        return null;
      }

      if (fileDataHandler == null) {
        if (saveCrashedFilesProperty) {
          DebugFileWriter.saveDebugFile("/logs/crashedfiles/", fileContent,
              file.fileName);
        }
      }
      return fileDataHandler;

    } catch (NoSuchElementException | NoSuchFieldError e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(e.toString());
      }
      return null;
    }
  }

  private String getUnambiguousUpstreamName(final Optional<String> repoRemoteUrl) {
    if (repoRemoteUrl.isPresent()) {
      // truncate https or anything else before the double slash
      String upstream = repoRemoteUrl.get();
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
