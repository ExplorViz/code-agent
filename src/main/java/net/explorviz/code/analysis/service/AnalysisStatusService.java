package net.explorviz.code.analysis.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory status tracking for analysis jobs
 */
@ApplicationScoped
public class AnalysisStatusService {

  private final Map<String, String> statusByLandscapeToken = new ConcurrentHashMap<>();

  public void markPending(final String landscapeToken) {
    statusByLandscapeToken.put(normalizeToken(landscapeToken), "pending");
  }

  public void markFinished(final String landscapeToken) {
    statusByLandscapeToken.put(normalizeToken(landscapeToken), "finished");
  }

  public void markFailed(final String landscapeToken) {
    statusByLandscapeToken.put(normalizeToken(landscapeToken), "failed");
  }

  public Optional<String> getStatus(final String landscapeToken) {
    return Optional.ofNullable(statusByLandscapeToken.get(normalizeToken(landscapeToken)));
  }

  private String normalizeToken(final String landscapeToken) {
    if (landscapeToken == null || landscapeToken.isBlank()) {
      return "unknown";
    }
    return landscapeToken;
  }
}
