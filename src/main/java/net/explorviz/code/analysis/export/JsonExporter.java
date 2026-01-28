package net.explorviz.code.analysis.export;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.explorviz.code.proto.CommitData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports the data into files in JSON format.
 */
public class JsonExporter implements DataExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonExporter.class);
  private static final String JSON_FILE_EXTENSION = ".json";
  private static final String[] SOURCE_FILE_EXTENSIONS = {
      ".java", ".ts", ".tsx", ".js", ".jsx", ".py"
  };

  private final String storageDirectory;
  private int commitCount;

  /**
   * Creates a JSON exporter that exports the data into folder based on the
   * current working folder and the given
   * application name.
   *
   * @param applicationName the name of the application
   * @throws IOException gets thrown if the needed directories were not created.
   */
  public JsonExporter(final String applicationName) throws IOException {
    String systemPath = System.getProperty("user.dir");
    systemPath = systemPath.replace("\\build\\classes\\java\\main", "");
    systemPath = systemPath.replace("/build/classes/java/main", "");
    this.storageDirectory = Paths.get(systemPath, "analysis-data", applicationName).toString();
    Files.createDirectories(Paths.get(storageDirectory));

    LOGGER.atInfo().addArgument(applicationName).addArgument(storageDirectory)
        .log("The analysis-data folder for application '{}' is created here: {}");

    this.commitCount = 0;
  }

  /**
   * Creates a JSON exporter that exports the data into folder given.
   *
   * @param pathToStorageDirectory the path to the JSON export folder
   */
  public JsonExporter(final java.nio.file.Path pathToStorageDirectory) {
    this.storageDirectory = pathToStorageDirectory.toString();
    this.commitCount = 0;
  }

  @Override
  public StateData getStateData(final String upstreamName, final String branchName,
      final String token,
      final String applicationName) {
    LOGGER.atInfo()
        .addArgument(upstreamName)
        .addArgument(branchName)
        .addArgument(applicationName)
        .log("📥 State data requested for {} on branch {} (application: {})");

    final StateData stateData = StateData.newBuilder().build();
    try {
      final String resultJson = JsonFormat.printer().print(stateData);
      final String requestJson = String.format(
          "{\n  \"upstreamName\": \"%s\",\n  \"branchName\": \"%s\",\n  \"landscapeToken\": \"%s\",\n  \"applicationName\": \"%s\"\n}",
          upstreamName, branchName, token, applicationName);

      final String stateFileName = "StateData_" + applicationName + JSON_FILE_EXTENSION;
      final String requestFileName = "StateRequest_" + applicationName + JSON_FILE_EXTENSION;

      Files.write(Paths.get(storageDirectory, stateFileName), resultJson.getBytes());
      Files.write(Paths.get(storageDirectory, requestFileName), requestJson.getBytes());

      LOGGER.atInfo()
          .addArgument(applicationName)
          .log("✅ Successfully exported state request and result for: {}");
    } catch (IOException e) {
      LOGGER.atError()
          .addArgument(applicationName)
          .addArgument(e.getMessage())
          .log("❌ Failed to export state data for {}: {}");
    }
    return stateData;
  }

  @Override
  public void persistFile(final FileData fileData) {
    try {
      LOGGER.atInfo()
          .addArgument(fileData.getFilePath())
          .addArgument(fileData.getLanguage())
          .log("📤 Exporting file data: {} (language: {})");

      final String json = JsonFormat.printer().print(fileData);

      // Remove file extension from filename
      String filePath = fileData.getFilePath();

      for (final String extension : SOURCE_FILE_EXTENSIONS) {
        if (filePath.endsWith(extension)) {
          filePath = filePath.substring(0, filePath.length() - extension.length());
          break;
        }
      }

      final String fileName = filePath + "_" + fileData.getFileHash() + JSON_FILE_EXTENSION;
      final var outputPath = Paths.get(storageDirectory, fileName);

      // Create parent directories if they don't exist
      // This is necessary because fileName may contain subdirectories
      // (e.g., "src/utils/file.json")
      if (outputPath.getParent() != null) {
        Files.createDirectories(outputPath.getParent());
      }

      Files.write(outputPath, json.getBytes());

      LOGGER.atInfo()
          .addArgument(fileName)
          .log("✅ Successfully exported file data to: {}");
    } catch (IOException e) { // NOPMD
      LOGGER.atError()
          .addArgument(fileData.getFilePath())
          .addArgument(e.getMessage())
          .log("❌ Failed to export file data for {}: {}");
      throw new RuntimeException(e); // NOPMD
    }
  }

  @Override
  public void persistCommit(final CommitData commitData) {
    try {
      final String json = JsonFormat.printer().print(commitData);
      final String fileName = "CommitReport_" + commitData.getCommitId() + "_" + commitCount
          + JSON_FILE_EXTENSION;
      Files.write(Paths.get(storageDirectory, fileName), json.getBytes());
    } catch (IOException e) { // NOPMD
      throw new RuntimeException(e); // NOPMD
    }
    this.commitCount++;
  }

  @Override
  public boolean isRemote() {
    return false;
  }

  @Override
  public boolean isInvalidCommitHash(final String hash) {
    return false;
  }
}
