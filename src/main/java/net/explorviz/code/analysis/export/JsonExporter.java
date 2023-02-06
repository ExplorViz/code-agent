package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

public class JsonExporter implements DataExporter {

  private String storageDirectory;

  public JsonExporter(String pathToStorageDirectory) {
    this.storageDirectory = pathToStorageDirectory;
  }

  @Override
  public StateData requestStateData(String branchName) {
    return StateData.newBuilder().build();
  }

  @Override
  public void sendFileData(FileData fileData) {
    //   saves FileData to json file
  }

  @Override
  public void sendCommitReport(CommitReportData commitReportData) {
    //   saves CommitReport to json file
  }
}
