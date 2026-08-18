package net.explorviz.code.analysis.service.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkOutputDirectoryTest {

  @TempDir Path tempDir;

  @Test
  void resolvesRelativePathAgainstWorkingDirectory() throws Exception {
    final BenchmarkOutputDirectory outputDirectory = new BenchmarkOutputDirectory();
    outputDirectory.benchmarkOutputDir = tempDir.resolve("benchmark-results").toString();

    final Path resolved = outputDirectory.resolveOutputDirectory();

    Assertions.assertEquals(
        tempDir.resolve("benchmark-results").normalize(), resolved.normalize());
    Assertions.assertTrue(Files.isDirectory(resolved));
  }

  @Test
  void keepsAbsolutePathUnchanged() throws Exception {
    final BenchmarkOutputDirectory outputDirectory = new BenchmarkOutputDirectory();
    outputDirectory.benchmarkOutputDir = tempDir.resolve("absolute-output").toString();

    final Path resolved = outputDirectory.resolveOutputDirectory();

    Assertions.assertTrue(resolved.isAbsolute());
    Assertions.assertTrue(Files.isDirectory(resolved));
  }
}
