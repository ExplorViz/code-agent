package net.explorviz.code.analysis.export;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves filesystem paths for local JSON analysis exports.
 */
public final class LocalAnalysisOutputDirectory {

  private static final String ANALYSIS_DATA_DIR = "analysis-data";

  private LocalAnalysisOutputDirectory() {}

  public static Path normalizeWorkingDirectory() {
    String systemPath = System.getProperty("user.dir");
    systemPath = systemPath.replace("\\build\\classes\\java\\main", "");
    systemPath = systemPath.replace("/build/classes/java/main", "");
    return Paths.get(systemPath);
  }

  public static Path resolveRepositoryOutputDirectory(final String repositoryName) {
    return normalizeWorkingDirectory().resolve(ANALYSIS_DATA_DIR).resolve(repositoryName);
  }

  public static Path resolveApplicationOutputDirectory(
      final String repositoryName, final String applicationName) {
    if (applicationName == null || applicationName.isBlank()) {
      return resolveRepositoryOutputDirectory(repositoryName);
    }
    return resolveRepositoryOutputDirectory(repositoryName).resolve(applicationName);
  }
}
