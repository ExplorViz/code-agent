package net.explorviz.code.analysis.handler;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.proto.CommitReportData;

@ApplicationScoped
public class CommitReportHandler {

  private CommitReportData.Builder builder;

  public CommitReportHandler() {
    this.builder = CommitReportData.newBuilder();
  }

  public void clear() {
    this.builder = CommitReportData.newBuilder();

  }

  public void init(String commitId, String branchName) {
    builder.setCommitID(commitId);
    builder.setBranchName(branchName);
  }

  public void add(FileDescriptor fileDescriptor) {
    builder.addFiles(fileDescriptor.relativePath);
  }

  public void add(List<FileDescriptor> fileDescriptorList) {
    for (FileDescriptor fileDescriptor : fileDescriptorList) {
      builder.addFiles(fileDescriptor.relativePath);
    }
  }

  public CommitReportData getCommitReport() {
    return builder.build();
  }
}
