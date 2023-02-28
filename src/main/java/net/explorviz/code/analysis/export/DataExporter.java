package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

/**
 * A DataExporter handles the export of {@link FileData}, {@link CommitReportData} and request of
 * {@link StateData}.
 */
public interface DataExporter {
  StateData requestStateData(final String branchName);

  void sendFileData(final FileData fileData);

  void sendCommitReport(final CommitReportData commitReportData);

  boolean isInvalidCommitHash(final String hash);
}