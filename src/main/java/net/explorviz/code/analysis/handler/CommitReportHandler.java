package net.explorviz.code.analysis.handler;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.proto.CommitReportData;

/**
 * The CommitReportHandler is used to create commit reports.
 */
@ApplicationScoped
public class CommitReportHandler {

  private CommitReportData.Builder builder;

  /**
   * Creates a blank handler, use {@link CommitReportHandler#init(String, String, String)} to
   * initialize it.
   */
  public CommitReportHandler() {
    this.builder = CommitReportData.newBuilder();
  }

  /**
   * Clears the commitReportData from old data entries. Gets called in
   * {@link CommitReportHandler#init(String, String, String)} automatically.
   */
  public void clear() {
    this.builder = CommitReportData.newBuilder();

  }

  /**
   * Initialize the current report handler.
   *
   * @param commitId the id of the commit
   * @param parentCommitId the id of the parent commit, can be null if no parent exists
   * @param branchName the name of the branch
   */
  public void init(final String commitId, final String parentCommitId, final String branchName) {
    clear();
    builder.setCommitID(commitId);
    builder.setParentCommitID(parentCommitId == null ? "NONE" : parentCommitId);
    builder.setBranchName(branchName);
  }

  public void add(final FileDescriptor fileDescriptor) {
    builder.addFiles(fileDescriptor.relativePath);
  }

  /**
   * Add multiple {@link FileDescriptor} to the report.
   *
   * @param fileDescriptorList the list of file descriptors to add to the report
   */
  public void add(final List<FileDescriptor> fileDescriptorList) {
    for (final FileDescriptor fileDescriptor : fileDescriptorList) {
      builder.addFiles(fileDescriptor.relativePath);
    }
  }

  public CommitReportData getCommitReport() {
    return builder.build();
  }
}
