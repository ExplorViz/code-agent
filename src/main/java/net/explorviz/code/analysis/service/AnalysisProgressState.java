package net.explorviz.code.analysis.service;

/**
 * In-memory representation of analysis state
 */
public record AnalysisProgressState(
    String status,
    int totalCommits,
    int analyzedCommits,
    int totalFiles,
    int analyzedFiles) {
}
