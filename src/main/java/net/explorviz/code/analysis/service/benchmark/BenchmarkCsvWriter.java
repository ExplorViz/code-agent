package net.explorviz.code.analysis.service.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

/**
 * Appends benchmark results to CSV files.
 */
public final class BenchmarkCsvWriter {

  private static final String COMMITS_HEADER =
      "run_id,commit_index,commit_hash,commit_date,analysis_time_ms,memory_consumption_mb";
  private static final String RUNS_HEADER =
      "run_id,total_analysis_time_ms,peak_memory_consumption_mb";

  private final Path commitsFile;
  private final Path runsFile;

  public BenchmarkCsvWriter(final Path outputDirectory) {
    this(outputDirectory.resolve("benchmark-commits.csv"), outputDirectory.resolve("benchmark-runs.csv"));
  }

  BenchmarkCsvWriter(final Path commitsFile, final Path runsFile) {
    this.commitsFile = commitsFile;
    this.runsFile = runsFile;
  }

  public void appendRunResults(
      final List<CommitBenchmarkResult> commitResults, final RunBenchmarkResult runResult)
      throws IOException {
    Files.createDirectories(commitsFile.getParent());
    appendCommitResults(commitResults);
    appendRunSummary(runResult);
  }

  private void appendCommitResults(final List<CommitBenchmarkResult> commitResults)
      throws IOException {
    final boolean writeHeader = !Files.exists(commitsFile) || Files.size(commitsFile) == 0;
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            commitsFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)) {
      if (writeHeader) {
        writer.write(COMMITS_HEADER);
        writer.newLine();
      }
      for (final CommitBenchmarkResult result : commitResults) {
        writer.write(
            String.format(
                Locale.US,
                "%d,%d,%s,%s,%.2f,%.1f",
                result.runId(),
                result.commitIndex(),
                result.commitHash(),
                result.commitDate(),
                result.analysisTimeMs(),
                result.memoryConsumptionMb()));
        writer.newLine();
      }
    }
  }

  private void appendRunSummary(final RunBenchmarkResult runResult) throws IOException {
    final boolean writeHeader = !Files.exists(runsFile) || Files.size(runsFile) == 0;
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            runsFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)) {
      if (writeHeader) {
        writer.write(RUNS_HEADER);
        writer.newLine();
      }
      writer.write(
          String.format(
              Locale.US,
              "%d,%.2f,%.1f",
              runResult.runId(),
              runResult.totalAnalysisTimeMs(),
              runResult.peakMemoryConsumptionMb()));
      writer.newLine();
    }
  }
}
