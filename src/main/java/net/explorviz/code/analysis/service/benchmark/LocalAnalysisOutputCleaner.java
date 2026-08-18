package net.explorviz.code.analysis.service.benchmark;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.explorviz.code.analysis.export.LocalAnalysisOutputDirectory;
import net.explorviz.code.analysis.service.AnalysisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes local JSON analysis output directories between benchmark runs.
 */
@ApplicationScoped
public class LocalAnalysisOutputCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalAnalysisOutputCleaner.class);

  public void deleteRepositoryAnalysisOutput(final AnalysisConfig config) throws IOException {
    final String repositoryName = config.getRepositoryName();
    if (repositoryName == null || repositoryName.isBlank()) {
      return;
    }

    deleteDirectory(LocalAnalysisOutputDirectory.resolveRepositoryOutputDirectory(repositoryName));
  }

  void deleteDirectory(final Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }

    try (Stream<Path> paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException exception) {
                  throw new UncheckedIOException(exception);
                }
              });
    } catch (UncheckedIOException exception) {
      throw exception.getCause();
    }

    LOGGER.info("Deleted local analysis output at {}", directory.toAbsolutePath());
  }
}
