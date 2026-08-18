package net.explorviz.code.analysis.service.benchmark;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Collects per-commit and per-run benchmark metrics during an analysis run.
 */
public final class BenchmarkMetricsCollector {

  private static final DateTimeFormatter COMMIT_DATE_FORMAT =
      DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

  private final int runId;
  private final List<CommitBenchmarkResult> commitResults = new ArrayList<>();
  private int commitIndex;
  private double totalAnalysisTimeMs;
  private double peakMemoryConsumptionMb;

  public BenchmarkMetricsCollector(final int runId) {
    this.runId = runId;
  }

  public int runId() {
    return runId;
  }

  public void recordCommit(
      final RevCommit commit, final long analysisTimeNanos, final double memoryConsumptionMb) {
    commitIndex++;
    final double analysisTimeMs = analysisTimeNanos / 1_000_000.0;
    totalAnalysisTimeMs += analysisTimeMs;
    peakMemoryConsumptionMb = Math.max(peakMemoryConsumptionMb, memoryConsumptionMb);

    final String shortHash =
        commit.getName().length() > 7 ? commit.getName().substring(0, 7) : commit.getName();
    final String commitDate =
        COMMIT_DATE_FORMAT.format(Instant.ofEpochSecond(commit.getCommitTime()));

    commitResults.add(
        new CommitBenchmarkResult(
            runId, commitIndex, shortHash, commitDate, analysisTimeMs, memoryConsumptionMb));
  }

  public List<CommitBenchmarkResult> commitResults() {
    return List.copyOf(commitResults);
  }

  public RunBenchmarkResult toRunResult() {
    return new RunBenchmarkResult(runId, totalAnalysisTimeMs, peakMemoryConsumptionMb);
  }

  public static double currentMemoryConsumptionMb() {
    final Runtime runtime = Runtime.getRuntime();
    return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
  }
}
