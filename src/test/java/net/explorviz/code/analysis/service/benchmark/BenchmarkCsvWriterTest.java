package net.explorviz.code.analysis.service.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkCsvWriterTest {

  @TempDir Path tempDir;

  @Test
  void writesCommitAndRunCsvWithHeaders() throws Exception {
    final Path commitsFile = tempDir.resolve("benchmark-commits.csv");
    final Path runsFile = tempDir.resolve("benchmark-runs.csv");
    final BenchmarkCsvWriter writer = new BenchmarkCsvWriter(commitsFile, runsFile);

    writer.appendRunResults(
        java.util.List.of(
            new CommitBenchmarkResult(1, 1, "a1b2c3d", "2025-01-10", 124.37, 184.6),
            new CommitBenchmarkResult(1, 2, "d4e5f6a", "2025-01-11", 98.21, 191.3)),
        new RunBenchmarkResult(1, 222.58, 191.3));

    final String commitsCsv = Files.readString(commitsFile);
    Assertions.assertTrue(commitsCsv.startsWith(
        "run_id,commit_index,commit_hash,commit_date,analysis_time_ms,memory_consumption_mb"));
    Assertions.assertTrue(commitsCsv.contains("1,1,a1b2c3d,2025-01-10,124.37,184.6"));
    Assertions.assertTrue(commitsCsv.contains("1,2,d4e5f6a,2025-01-11,98.21,191.3"));

    final String runsCsv = Files.readString(runsFile);
    Assertions.assertTrue(runsCsv.startsWith(
        "run_id,total_analysis_time_ms,peak_memory_consumption_mb"));
    Assertions.assertTrue(runsCsv.contains("1,222.58,191.3"));
  }

  @Test
  void appendsWithoutDuplicateHeaders() throws Exception {
    final Path commitsFile = tempDir.resolve("benchmark-commits.csv");
    final Path runsFile = tempDir.resolve("benchmark-runs.csv");
    final BenchmarkCsvWriter writer = new BenchmarkCsvWriter(commitsFile, runsFile);

    writer.appendRunResults(
        java.util.List.of(new CommitBenchmarkResult(1, 1, "a1b2c3d", "2025-01-10", 124.37, 184.6)),
        new RunBenchmarkResult(1, 124.37, 184.6));
    writer.appendRunResults(
        java.util.List.of(new CommitBenchmarkResult(2, 1, "a1b2c3d", "2025-01-10", 120.00, 180.0)),
        new RunBenchmarkResult(2, 120.00, 180.0));

    final String commitsCsv = Files.readString(commitsFile);
    Assertions.assertEquals(1, commitsCsv.lines().filter(line -> line.startsWith("run_id,")).count());
    Assertions.assertTrue(commitsCsv.contains("2,1,a1b2c3d,2025-01-10,120.00,180.0"));
  }
}
