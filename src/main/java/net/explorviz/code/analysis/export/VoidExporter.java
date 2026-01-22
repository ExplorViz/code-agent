package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

/**
 * Dummy to dump the data into void.
 */
public class VoidExporter implements DataExporter {

  @Override
  public StateData requestStateData(final String upstreamName, final String branchName,
      final String token,
      final String applicationName) {
    return StateData.newBuilder().build();
  }

  @Override
  public void sendFileData(final FileData fileData) {
    // DO NOTHING
  }

  @Override
  public void sendCommitReport(final CommitReportData commitReportData) {
    // DO NOTHING
  }

  @Override
  public boolean isInvalidCommitHash(final String hash) {
    return false;
  }
}
