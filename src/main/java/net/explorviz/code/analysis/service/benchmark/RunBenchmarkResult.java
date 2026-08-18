package net.explorviz.code.analysis.service.benchmark;

/**
 * Summary metrics for a complete benchmark run.
 */
public record RunBenchmarkResult(
    int runId, double totalAnalysisTimeMs, double peakMemoryConsumptionMb) {}
