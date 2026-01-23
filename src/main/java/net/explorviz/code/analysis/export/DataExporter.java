package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.StateData;

/**
 * A DataExporter handles the export of {@link FileData}, {@link CommitData} and request of
 * {@link StateData}.
 */
public interface DataExporter {

  StateData requestStateData(final String upstreamName, final String branchName, final String token,
      final String applicationName);

  void sendFileData(final FileData fileData);

  void sendCommitReport(final CommitData commitData);

  boolean isInvalidCommitHash(final String hash);
}
