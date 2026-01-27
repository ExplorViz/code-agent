package net.explorviz.code.analysis;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.export.GrpcExporter;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.service.AnalysisConfig;
import net.explorviz.code.analysis.service.AnalysisService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint for this service. Expects a local path to a Git repository folder ("explorviz.repo.folder.path"). Sends
 * the analysis's results to ExplorViz code service.
 */
@ApplicationScoped
public class GitAnalysis { // NOPMD

  private static final Logger LOGGER = LoggerFactory.getLogger(GitAnalysis.class);

  private static final int ONE_SECOND_IN_MILLISECONDS = 1000;

  @ConfigProperty(name = "explorviz.gitanalysis.run-mode")
  /* default */ Optional<String> runMode; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.local.storage-path")
  /* default */ Optional<String> repoPathProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.url")
  /* default */ Optional<String> repoRemoteUrlProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.username")
  /* default */ Optional<String> usernameProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.password")
  /* default */ Optional<String> passwordProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.branch")
  /* default */ Optional<String> repositoryBranchProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.source-directory")
  /* default */ Optional<String> sourceDirectoryProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.restrict-analysis-to-folders")
  /* default */ Optional<String> restrictAnalysisToFoldersProperty; // NOCS NOPMD

  @ConfigProperty(name = "explorviz.gitanalysis.send-to-remote", defaultValue = "true")
  /* default */ boolean sendToRemoteProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.calculate-metrics", defaultValue = "true")
  /* default */ boolean calculateMetricsProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.fetch-remote-data", defaultValue = "true")
  /* default */ boolean fetchRemoteDataProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.start-commit-sha1")
  /* default */ Optional<String> startCommitProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.end-commit-sha1")
  /* default */ Optional<String> endCommitProperty; // NOCS

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeTokenProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.application-name")
  /* default */ String applicationNameProperty; // NOCS

  @Inject
  /* package */ GrpcExporter grpcExporter; // NOCS

  @Inject
  /* package */ AnalysisService analysisService; // NOCS

  /**
   * Creates an AnalysisConfig from the current properties.
   *
   * @return The analysis configuration
   */
  private AnalysisConfig createConfig() {
    return new AnalysisConfig.Builder()
        .repoPath(repoPathProperty)
        .repoRemoteUrl(repoRemoteUrlProperty)
        .gitUsername(usernameProperty)
        .gitPassword(passwordProperty)
        .branch(repositoryBranchProperty)
        .sourceDirectory(sourceDirectoryProperty)
        .restrictAnalysisToFolders(restrictAnalysisToFoldersProperty)
        .calculateMetrics(calculateMetricsProperty)
        .startCommit(startCommitProperty)
        .endCommit(endCommitProperty)
        .landscapeToken(landscapeTokenProperty)
        .applicationName(applicationNameProperty)
        .fetchRemoteData(fetchRemoteDataProperty)
        .build();
  }

  private void analyzeAndSendRepo(final DataExporter exporter) // NOCS NOPMD
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException { // NOPMD
    final AnalysisConfig config = createConfig();
    analysisService.analyzeAndSendRepo(config, exporter);
  }

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, GitAPIException, PropertyNotDefinedException, NotFoundException {

    if (runMode.isPresent() && "api".equals(runMode.get())) {
      LOGGER.info("Running in API mode");
      return;
    }

    final long startTime = System.currentTimeMillis();

    if (repoPathProperty.isEmpty() && repoRemoteUrlProperty.isEmpty()) {
      return;
    }
    DataExporter exporter;
    if (sendToRemoteProperty) {
      exporter = grpcExporter;
    } else {
      exporter = new JsonExporter(applicationNameProperty);
    }
    analyzeAndSendRepo(exporter);

    final long endTime = System.currentTimeMillis();

    LOGGER.atInfo().addArgument((endTime - startTime) / ONE_SECOND_IN_MILLISECONDS)
        .log("Analysis finished successfully and took {} seconds, exiting now. ");

    Quarkus.asyncExit();
    // Quarkus.waitForExit();
    // System.exit(-1); // NOPMD

  }

}
