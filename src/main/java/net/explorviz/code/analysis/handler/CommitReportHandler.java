package net.explorviz.code.analysis.handler;

import com.google.protobuf.Timestamp;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.proto.CommitData;
import net.explorviz.code.proto.FileIdentifier;

/**
 * The CommitReportHandler is used to create commit reports.
 */
@ApplicationScoped
public class CommitReportHandler { // NOPMD

  private final Map<String, FileDescriptor> allFiles = new HashMap<>();
  private final List<String> modifiedFiles = new ArrayList<>();
  private final List<String> deletedFiles = new ArrayList<>();
  private final List<String> addedFiles = new ArrayList<>();
  private CommitData.Builder builder;

  /**
   * Creates a blank handler, use {@link CommitReportHandler#init(String, String, String)} to initialize it.
   */
  public CommitReportHandler() {
    this.builder = CommitData.newBuilder();
  }

  /**
   * Clears the commitReportData from old data entries. Gets called in
   * {@link CommitReportHandler#init(String, String, String)} automatically.
   */
  public void clear() {
    this.builder = CommitData.newBuilder();
    this.allFiles.clear();
    this.modifiedFiles.clear();
    this.deletedFiles.clear();
    this.addedFiles.clear();
  }

  /**
   * Initialize the current report handler.
   *
   * @param commitId       the id of the commit
   * @param parentCommitId the id of the parent commit, can be null if no parent exists
   * @param branchName     the name of the branch
   */
  public void init(final String commitId, final String parentCommitId, final String branchName) {
    clear();
    builder.setCommitId(commitId);
    builder.setParentCommitId(parentCommitId == null ? "NONE" : parentCommitId);
    builder.setBranchName(branchName);
  }

  public void add(final FileDescriptor fileDescriptor) {
    this.allFiles.put(fileDescriptor.relativePath, fileDescriptor);
  }

  /**
   * Add multiple {@link FileDescriptor} to the report.
   *
   * @param fileDescriptorList the list of file descriptors to add to the report
   */
  public void add(final List<FileDescriptor> fileDescriptorList) {
    for (final FileDescriptor fileDescriptor : fileDescriptorList) {
      this.add(fileDescriptor);
    }
  }

  private String getFileHash(final FileDescriptor fileDescriptor) {
    String s = fileDescriptor.objectId.toString();
    if (s.contains("[") && s.contains("]")) {
      final String[] sa = s.split("\\[");
      return sa[1].substring(0, sa[1].length() - 1);
    }
    return s;
  }

  public void addModified(final FileDescriptor fileDescriptor) {
    modifiedFiles.add(fileDescriptor.relativePath);
  }

  public void addDeleted(final FileDescriptor fileDescriptor) {
    deletedFiles.add(fileDescriptor.relativePath);
  }

  public void addAdded(final FileDescriptor fileDescriptor) {
    addedFiles.add(fileDescriptor.relativePath);
  }

  /**
   * ...
   */
  public void addTags(final List<String> tags) {
    builder.addAllTags(tags);
  }

  public void addToken(final String token) {
    builder.setLandscapeToken(token);
  }

  public void setRepositoryName(final String repositoryName) {
    builder.setRepositoryName(repositoryName);
  }

  public void setAuthorDate(final Timestamp authorDate) {
    builder.setAuthorDate(authorDate);
  }

  public void setCommitDate(final Timestamp commitDate) {
    builder.setCommitDate(commitDate);
  }

  /**
   * Returns the commit data. * * @return commit data object
   */
  public CommitData getCommitData() {
    for (Map.Entry<String, FileDescriptor> entry : allFiles.entrySet()) {
      FileIdentifier fileId = FileIdentifier.newBuilder()
          .setFilePath(entry.getValue().relativePath)
          .setFileHash(getFileHash(entry.getValue()))
          .build();

      if (addedFiles.contains(entry.getKey())) {
        builder.addAddedFiles(fileId);
      } else if (modifiedFiles.contains(entry.getKey())) {
        builder.addModifiedFiles(fileId);
      } else if (deletedFiles.contains(entry.getKey())) {
        builder.addDeletedFiles(fileId);
      } else {
        builder.addUnchangedFiles(fileId);
      }
    }
    return builder.build();
  }
}
