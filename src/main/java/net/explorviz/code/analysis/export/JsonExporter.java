package net.explorviz.code.analysis.export;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

public class JsonExporter implements DataExporter {

  private final String storageDirectory;
  private int commitCount;

  public JsonExporter() throws IOException {
    this.storageDirectory = Paths.get(System.getProperty("user.dir"), "analysis-data").toString();
    Files.createDirectories(Paths.get(storageDirectory));
    this.commitCount = 0;
  }

  public JsonExporter(final String pathToStorageDirectory) {
    this.storageDirectory = pathToStorageDirectory;
    this.commitCount = 0;
  }

  @Override
  public StateData requestStateData(final String branchName) {
    return StateData.newBuilder().build();
  }

  @Override
  public void sendFileData(final FileData fileData) {
    try {
      String json = JsonFormat.printer().print(fileData);
      String fileName =
          fileData.getFileName().replaceAll(".java", "_") + fileData.getCommitID() + ".json";
      Files.write(Paths.get(storageDirectory, fileName), json.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendCommitReport(final CommitReportData commitReportData) {
    try {
      String json = JsonFormat.printer().print(commitReportData);
      String fileName =
          "CommitReport_" + commitReportData.getCommitID() + "_" + commitCount + ".json";
      Files.write(Paths.get(storageDirectory, fileName), json.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.commitCount++;
  }
}
