package net.explorviz.code.analysis.export;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports the data into files in json format.
 */
public class JsonExporter implements DataExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonExporter.class);
  private static final String JSON_FILE_EXTENSION = ".json";
  private static final String JAVA_FILE_EXTENSION = ".java";
  private static final String[] SOURCE_FILE_EXTENSIONS = {".java", ".ts", ".tsx", ".js", ".jsx", ".py"};

  private final String storageDirectory;
  private int commitCount;

  /**
   * Creates a json exporter that exports the data into folder based on the current working folder.
   *
   * @throws IOException gets thrown if the needed directories were not created.
   */
  public JsonExporter() throws IOException {
    String systemPath = System.getProperty("user.dir");
    systemPath = systemPath.replace("\\build\\classes\\java\\main", "");
    systemPath = systemPath.replace("/build/classes/java/main", "");
    this.storageDirectory = Paths.get(systemPath, "analysis-data").toString();
    Files.createDirectories(Paths.get(storageDirectory));

    LOGGER.atInfo().addArgument(storageDirectory)
        .log("The analysis-data folder is created here: {}");

    this.commitCount = 0;
  }

  /**
   * Creates a json exporter that exports the data into folder given.
   *
   * @param pathToStorageDirectory the path to the json export folder
   */
  public JsonExporter(final String pathToStorageDirectory) {
    this.storageDirectory = pathToStorageDirectory;
    this.commitCount = 0;
  }

  @Override
  public StateData requestStateData(final String upstreamName, final String branchName) {
    return StateData.newBuilder().build();
  }

  @Override
  public void sendFileData(final FileData fileData) {
    try {
      LOGGER.atInfo()
          .addArgument(fileData.getFileName())
          .addArgument(fileData.getLanguage())
          .log("📤 Exporting file data: {} (language: {})");
      
      final String json = JsonFormat.printer().print(fileData);
      
      // Remove file extension from filename
      String baseFileName = fileData.getFileName();
      for (final String extension : SOURCE_FILE_EXTENSIONS) {
        if (baseFileName.endsWith(extension)) {
          baseFileName = baseFileName.substring(0, baseFileName.length() - extension.length());
          break;
        }
      }
      
      final String fileName = baseFileName + "_" + fileData.getCommitID() + JSON_FILE_EXTENSION;
      Files.write(Paths.get(storageDirectory, fileName), json.getBytes());
      
      LOGGER.atInfo()
          .addArgument(fileName)
          .log("✅ Successfully exported file data to: {}");
    } catch (IOException e) { // NOPMD
      LOGGER.atError()
          .addArgument(fileData.getFileName())
          .addArgument(e.getMessage())
          .log("❌ Failed to export file data for {}: {}");
      throw new RuntimeException(e); // NOPMD
    }
  }

  @Override
  public void sendCommitReport(final CommitReportData commitReportData) {
    try {
      final String json = JsonFormat.printer().print(commitReportData);
      final String fileName =
          "CommitReport_" + commitReportData.getCommitID() + "_" + commitCount
              + JSON_FILE_EXTENSION;
      Files.write(Paths.get(storageDirectory, fileName), json.getBytes());
    } catch (IOException e) { // NOPMD
      throw new RuntimeException(e); // NOPMD
    }
    this.commitCount++;
  }

  @Override
  public boolean isInvalidCommitHash(final String hash) {
    return false;
  }
}
