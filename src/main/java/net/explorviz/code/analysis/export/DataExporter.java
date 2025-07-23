package net.explorviz.code.analysis.export;

import net.explorviz.code.proto.CommitReportData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.FileRequest;
import net.explorviz.code.proto.FileResponse;
import net.explorviz.code.proto.StateData;

/**
 * A DataExporter handles the export of {@link FileData},
 * {@link CommitReportData} and request of
 * {@link StateData} as well as {@link FileRespone}.
 */
public interface DataExporter {

  StateData requestStateData(final String upstreamName, final String branchName);

  void sendFileData(final FileData fileData);

  FileResponse getFileNames(final FileRequest fileRequest);

  void sendCommitReport(final CommitReportData commitReportData);

  boolean isInvalidCommitHash(final String hash);
}
