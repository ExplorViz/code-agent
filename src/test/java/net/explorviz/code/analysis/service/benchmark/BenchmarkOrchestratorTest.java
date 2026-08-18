package net.explorviz.code.analysis.service.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import net.explorviz.code.analysis.export.DataExporter;
import net.explorviz.code.analysis.service.AnalysisConfig;
import net.explorviz.code.analysis.service.AnalysisService;
import net.explorviz.code.analysis.service.AnalysisStatusService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class BenchmarkOrchestratorTest {

  @TempDir Path tempDir;

  @Test
  void writesCsvFilesEvenWhenAnalysisFails() throws Exception {
    final AnalysisService analysisService = Mockito.mock(AnalysisService.class);
    Mockito.doThrow(new RuntimeException("analysis failed"))
        .when(analysisService)
        .analyzeAndSendRepo(
            Mockito.any(AnalysisConfig.class),
            Mockito.any(DataExporter.class),
            Mockito.any(BenchmarkMetricsCollector.class));

    final BenchmarkOrchestrator orchestrator = createOrchestrator(analysisService, tempDir);

    final AnalysisConfig config =
        new AnalysisConfig.Builder()
            .landscapeToken("token")
            .benchmarkMode(true)
            .benchmarkRepeatCount(java.util.Optional.of(1))
            .build();

    Assertions.assertThrows(
        RuntimeException.class,
        () -> orchestrator.runBenchmark(config, remoteExporter()));

    Assertions.assertTrue(Files.exists(tempDir.resolve("benchmark-commits.csv")));
    Assertions.assertTrue(Files.exists(tempDir.resolve("benchmark-runs.csv")));
  }

  @Test
  void purgesDatabaseBeforeEachRemoteRun() throws Exception {
    final AnalysisService analysisService = Mockito.mock(AnalysisService.class);
    final LandscapePurgeClient purgeClient = Mockito.mock(LandscapePurgeClient.class);

    final BenchmarkOrchestrator orchestrator = createOrchestrator(analysisService, tempDir);
    orchestrator.landscapePurgeClient = purgeClient;

    final AnalysisConfig config =
        new AnalysisConfig.Builder()
            .landscapeToken("token")
            .benchmarkMode(true)
            .benchmarkRepeatCount(java.util.Optional.of(2))
            .build();

    orchestrator.runBenchmark(config, remoteExporter());

    Mockito.verify(purgeClient, Mockito.times(2)).purgeDatabase();
    Assertions.assertTrue(Files.exists(tempDir.resolve("benchmark-runs.csv")));
  }

  @Test
  void deletesLocalAnalysisOutputAfterEachRun() throws Exception {
    final AnalysisService analysisService = Mockito.mock(AnalysisService.class);
    final LocalAnalysisOutputCleaner localCleaner = Mockito.mock(LocalAnalysisOutputCleaner.class);

    final BenchmarkOrchestrator orchestrator = createOrchestrator(analysisService, tempDir);
    orchestrator.localAnalysisOutputCleaner = localCleaner;

    final AnalysisConfig config =
        new AnalysisConfig.Builder()
            .repoRemoteUrl(java.util.Optional.of("https://github.com/org/spring-petclinic.git"))
            .landscapeToken("token")
            .benchmarkMode(true)
            .benchmarkRepeatCount(java.util.Optional.of(2))
            .build();

    orchestrator.runBenchmark(config, localExporter());

    Mockito.verify(localCleaner, Mockito.times(4)).deleteRepositoryAnalysisOutput(config);
  }

  private static DataExporter remoteExporter() {
    final DataExporter exporter = Mockito.mock(DataExporter.class);
    Mockito.when(exporter.isRemote()).thenReturn(true);
    return exporter;
  }

  private static DataExporter localExporter() {
    final DataExporter exporter = Mockito.mock(DataExporter.class);
    Mockito.when(exporter.isRemote()).thenReturn(false);
    return exporter;
  }

  private static BenchmarkOrchestrator createOrchestrator(
      final AnalysisService analysisService, final Path outputDir) {
    final BenchmarkOutputDirectory outputDirectory = new BenchmarkOutputDirectory();
    outputDirectory.benchmarkOutputDir = outputDir.toString();

    final BenchmarkOrchestrator orchestrator = new BenchmarkOrchestrator();
    orchestrator.analysisService = analysisService;
    orchestrator.landscapePurgeClient = Mockito.mock(LandscapePurgeClient.class);
    orchestrator.localAnalysisOutputCleaner = Mockito.mock(LocalAnalysisOutputCleaner.class);
    orchestrator.analysisStatusService = Mockito.mock(AnalysisStatusService.class);
    orchestrator.benchmarkOutputDirectory = outputDirectory;
    return orchestrator;
  }
}
