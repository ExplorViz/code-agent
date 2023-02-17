package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

public class VoidExporter implements DataExporter {
  @Override
  public StateData requestStateData(String branchName) {
    return StateData.newBuilder().build();
  }

  @Override
  public void sendFileData(FileData fileData) {
  }

  @Override
  public void sendCommitReport(CommitReportData commitReportData) {
  }
}
