package net.explorviz.code.analysis.service.benchmark;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.export.JsonExporter;
import net.explorviz.code.analysis.service.AnalysisConfig;
import net.explorviz.code.analysis.service.AnalysisService;
import net.explorviz.code.analysis.service.AnalysisStatusService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs repeated analysis executions for benchmarking. Remote runs purge the landscape database before
 * each iteration; local runs delete the analysis-data folder after each iteration.
 */
@ApplicationScoped
public class BenchmarkOrchestrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkOrchestrator.class);

  @Inject AnalysisService analysisService;

  @Inject LandscapePurgeClient landscapePurgeClient;

  @Inject LocalAnalysisOutputCleaner localAnalysisOutputCleaner;

  @Inject AnalysisStatusService analysisStatusService;

  @Inject BenchmarkOutputDirectory benchmarkOutputDirectory;

  public Path runBenchmark(final AnalysisConfig config, final DataExporter exporter)
      throws IOException, GitAPIException, NotFoundException, PropertyNotDefinedException {
    final int repeatCount = config.benchmarkRepeatCount().orElse(1);
    final Path outputDirectory = benchmarkOutputDirectory.resolveOutputDirectory();
    final BenchmarkCsvWriter csvWriter = new BenchmarkCsvWriter(outputDirectory);

    LOGGER.info(
        "Starting benchmark mode with {} run(s) using {} export. CSV files: {} and {}",
        repeatCount,
        exporter.isRemote() ? "remote" : "local",
        outputDirectory.resolve("benchmark-commits.csv"),
        outputDirectory.resolve("benchmark-runs.csv"));

    for (int runId = 1; runId <= repeatCount; runId++) {
      LOGGER.info("Starting benchmark run {}/{}", runId, repeatCount);
      prepareForBenchmarkRun(config, exporter, runId);

      final DataExporter runExporter = createExporterForRun(config, exporter);
      analysisStatusService.markRunning(config.landscapeToken(), 0, 0);

      final BenchmarkMetricsCollector collector = new BenchmarkMetricsCollector(runId);
      try {
        analysisService.analyzeAndSendRepo(config, runExporter, collector);
      } finally {
        csvWriter.appendRunResults(collector.commitResults(), collector.toRunResult());
        LOGGER.info(
            "Benchmark run {} CSV updated ({} commit rows). Files at {}",
            runId,
            collector.commitResults().size(),
            outputDirectory.toAbsolutePath());
      }

      cleanupAfterBenchmarkRun(config, exporter);

      LOGGER.info(
          "Benchmark run {} completed: total time {} ms, peak memory {} MB",
          runId,
          String.format("%.2f", collector.toRunResult().totalAnalysisTimeMs()),
          String.format("%.1f", collector.toRunResult().peakMemoryConsumptionMb()));
    }

    LOGGER.info("Benchmark completed. Results written to {}", outputDirectory.toAbsolutePath());
    return outputDirectory;
  }

  private void prepareForBenchmarkRun(
      final AnalysisConfig config, final DataExporter exporter, final int runId)
      throws IOException {
    if (exporter.isRemote()) {
      try {
        landscapePurgeClient.purgeDatabase();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IOException("Landscape purge interrupted before benchmark run " + runId, exception);
      }
      return;
    }

    if (!exporter.isRemote()) {
      localAnalysisOutputCleaner.deleteRepositoryAnalysisOutput(config);
    }
  }

  private DataExporter createExporterForRun(final AnalysisConfig config, final DataExporter exporter)
      throws IOException {
    if (exporter.isRemote()) {
      return exporter;
    }
    return new JsonExporter(config.getRepositoryName(), config.primaryApplicationNameForExport());
  }

  private void cleanupAfterBenchmarkRun(final AnalysisConfig config, final DataExporter exporter)
      throws IOException {
    if (!exporter.isRemote()) {
      localAnalysisOutputCleaner.deleteRepositoryAnalysisOutput(config);
    }
  }
}
