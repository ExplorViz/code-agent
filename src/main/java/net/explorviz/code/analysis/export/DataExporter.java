package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

public interface DataExporter {
  StateData requestStateData(final String branchName);

  void sendFileData(final FileData fileData);

  void sendCommitReport(final CommitReportData commitReportData);
}