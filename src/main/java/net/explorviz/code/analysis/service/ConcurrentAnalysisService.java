package net.explorviz.code.analysis.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.export.DataExporter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper service that handles concurrent analysis requests safely. Uses an executor service to queue and process
 * analysis requests sequentially
 */
@ApplicationScoped
public class ConcurrentAnalysisService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentAnalysisService.class);

  @Inject
  /* package */ AnalysisService analysisService; // NOCS

  // Single-threaded executor to process analysis requests sequentially
  private ExecutorService executorService;

  @PostConstruct
  public void init() {
    executorService = Executors.newSingleThreadExecutor(r -> {
      final Thread thread = new Thread(r, "code-analysis-worker");
      thread.setDaemon(false); // Keep thread alive for pending requests
      return thread;
    });
    LOGGER.info("ConcurrentAnalysisService initialized with sequential request processing");
  }

  @PreDestroy
  public void shutdown() {
    LOGGER.info("⏳ Shutting down ConcurrentAnalysisService...");
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        LOGGER.warn("⚠️  Executor did not terminate in time, forcing shutdown");
        executorService.shutdownNow();
      } else {
        LOGGER.info("✅ ConcurrentAnalysisService shut down successfully");
      }
    } catch (InterruptedException e) {
      LOGGER.error("❌ Interrupted during shutdown", e);
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  public CompletableFuture<Void> analyzeAndSendRepoAsync(final AnalysisConfig config,
      final DataExporter exporter) {
    final String repoUrl = config.repoRemoteUrl().orElse("unknown");

    LOGGER.info("📥 Queuing analysis request for repository: {}", repoUrl);

    return CompletableFuture.runAsync(() -> {
      try {
        LOGGER.info("⚙️  Processing analysis request for repository: {}", repoUrl);
        analysisService.analyzeAndSendRepo(config, exporter);
        LOGGER.info("✅ Completed analysis for repository: {}", repoUrl);
      } catch (IOException | GitAPIException | NotFoundException
          | PropertyNotDefinedException e) {
        LOGGER.error("❌ Analysis failed for repository: {}", repoUrl, e);
        throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
      }
    }, executorService);
  }
}
