package net.explorviz.code.analysis.service.benchmark;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves and prepares the directory where benchmark CSV files are written.
 */
@ApplicationScoped
public class BenchmarkOutputDirectory {

  private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkOutputDirectory.class);

  @ConfigProperty(name = "explorviz.gitanalysis.benchmark.output-dir", defaultValue = "benchmark-results")
  /* default */ String benchmarkOutputDir;

  public Path resolveOutputDirectory() throws java.io.IOException {
    final Path configuredPath = Paths.get(benchmarkOutputDir);
    final Path outputDirectory =
        configuredPath.isAbsolute()
            ? configuredPath.normalize()
            : Paths.get(System.getProperty("user.dir")).resolve(configuredPath).normalize();
    Files.createDirectories(outputDirectory);
    LOGGER.info("Benchmark CSV output directory: {}", outputDirectory);
    return outputDirectory;
  }
}
