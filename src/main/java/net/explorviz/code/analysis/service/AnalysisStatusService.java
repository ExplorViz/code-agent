package net.explorviz.code.analysis.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory status tracking for analysis jobs
 */
@ApplicationScoped
public class AnalysisStatusService {

  private static final String STATUS_PENDING = "pending";
  private static final String STATUS_RUNNING = "running";
  private static final String STATUS_FINISHED = "finished";
  private static final String STATUS_FAILED = "failed";
  private static final String UNKNOWN_TOKEN = "unknown";

  private final Map<String, AnalysisProgressState> stateByLandscapeToken = new ConcurrentHashMap<>();

  public void markPending(final String landscapeToken) {
    stateByLandscapeToken.put(normalizeToken(landscapeToken), emptyState(STATUS_PENDING));
  }

  public void markRunning(final String landscapeToken, final int totalCommits,
      final int totalFiles) {
    upsertState(landscapeToken, current -> {
      final AnalysisProgressState previous = current == null ? emptyState(STATUS_PENDING) : current;
      return new AnalysisProgressState(STATUS_RUNNING, totalCommits, previous.analyzedCommits(),
          totalFiles, previous.analyzedFiles());
    });
  }

  public void incrementAnalyzedCommit(final String landscapeToken) {
    updateExistingState(landscapeToken, state ->
      new AnalysisProgressState(state.status(), state.totalCommits(),
          state.analyzedCommits() + 1, state.totalFiles(), state.analyzedFiles()));
  }

  public void setCurrentCommitFiles(final String landscapeToken, final int totalFiles) {
    updateExistingState(landscapeToken, state ->
      new AnalysisProgressState(state.status(), state.totalCommits(),
          state.analyzedCommits(), Math.max(0, totalFiles), 0));
  }

  public void incrementAnalyzedFile(final String landscapeToken) {
    updateExistingState(landscapeToken, state ->
      new AnalysisProgressState(state.status(), state.totalCommits(),
          state.analyzedCommits(), state.totalFiles(), state.analyzedFiles() + 1));
  }

  public void markFinished(final String landscapeToken) {
    upsertState(landscapeToken, current -> {
      if (current == null) {
        return emptyState(STATUS_FINISHED);
      }
      return new AnalysisProgressState(STATUS_FINISHED, current.totalCommits(),
          current.totalCommits(), current.totalFiles(), current.totalFiles());
    });
  }

  public void markFailed(final String landscapeToken) {
    upsertState(landscapeToken, current -> {
      if (current == null) {
        return emptyState(STATUS_FAILED);
      }
      return new AnalysisProgressState(STATUS_FAILED, current.totalCommits(),
          current.analyzedCommits(), current.totalFiles(), current.analyzedFiles());
    });
  }

  public Optional<String> getStatus(final String landscapeToken) {
    return getState(landscapeToken).map(AnalysisProgressState::status);
  }

  public Optional<AnalysisProgressState> getState(final String landscapeToken) {
    return Optional.ofNullable(stateByLandscapeToken.get(normalizeToken(landscapeToken)));
  }

  private void updateExistingState(final String landscapeToken,
      final Function<AnalysisProgressState, AnalysisProgressState> update) {
    stateByLandscapeToken.computeIfPresent(normalizeToken(landscapeToken),
        (token, current) -> update.apply(current));
  }

  private void upsertState(final String landscapeToken,
      final Function<AnalysisProgressState, AnalysisProgressState> update) {
    stateByLandscapeToken.compute(normalizeToken(landscapeToken),
        (token, current) -> update.apply(current));
  }

  private AnalysisProgressState emptyState(final String status) {
    return new AnalysisProgressState(status, 0, 0, 0, 0);
  }

  private String normalizeToken(final String landscapeToken) {
    if (landscapeToken == null || landscapeToken.isBlank()) {
      return UNKNOWN_TOKEN;
    }
    return landscapeToken;
  }
}
