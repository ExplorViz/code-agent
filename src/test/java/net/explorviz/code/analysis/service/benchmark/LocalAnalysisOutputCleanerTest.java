package net.explorviz.code.analysis.service.benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import net.explorviz.code.analysis.export.LocalAnalysisOutputDirectory;
import net.explorviz.code.analysis.service.AnalysisConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalAnalysisOutputCleanerTest {

  @TempDir Path tempDir;

  @Test
  void deletesRepositoryAnalysisOutputDirectory() throws Exception {
    final Path repositoryDir =
        tempDir.resolve("analysis-data").resolve("spring-petclinic").resolve("app");
    Files.createDirectories(repositoryDir);
    Files.writeString(repositoryDir.resolve("CommitReport_abc_0.json"), "{}");

    final LocalAnalysisOutputCleaner cleaner = new LocalAnalysisOutputCleaner();
    cleaner.deleteDirectory(tempDir.resolve("analysis-data").resolve("spring-petclinic"));

    Assertions.assertFalse(Files.exists(repositoryDir));
  }

  @Test
  void deleteRepositoryAnalysisOutputUsesRepositoryNameFromConfig() throws Exception {
    final Path repositoryDir = tempDir.resolve("analysis-data").resolve("demo-repo");
    Files.createDirectories(repositoryDir);
    Files.writeString(repositoryDir.resolve("file.json"), "{}");

    final String originalUserDir = System.getProperty("user.dir");
    System.setProperty("user.dir", tempDir.toString());
    try {
      final LocalAnalysisOutputCleaner cleaner = new LocalAnalysisOutputCleaner();
      final AnalysisConfig config =
          new AnalysisConfig.Builder()
              .repoRemoteUrl(java.util.Optional.of("https://github.com/org/demo-repo.git"))
              .landscapeToken("token")
              .build();
      cleaner.deleteRepositoryAnalysisOutput(config);
    } finally {
      System.setProperty("user.dir", originalUserDir);
    }

    Assertions.assertFalse(Files.exists(repositoryDir));
  }
}
