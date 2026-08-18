package net.explorviz.code.analysis.service.benchmark;

/**
 * Per-commit benchmark metrics collected during a single analysis run.
 */
public record CommitBenchmarkResult(
    int runId,
    int commitIndex,
    String commitHash,
    String commitDate,
    double analysisTimeMs,
    double memoryConsumptionMb) {}
