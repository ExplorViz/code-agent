package net.explorviz.code.analysis.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.proto.CommitReportData;
//import org.slf4j.LoggerFactory;

/**
 * The CommitReportHandler is used to create commit reports.
 */
@ApplicationScoped
public class CommitReportHandler {

  private CommitReportData.Builder builder;
  private final Map<String, FileMetricHandler> fileNameToFileMetricHandlerMap;

  /**
   * Creates a blank handler, use {@link CommitReportHandler#init(String, String, String)} to
   * initialize it.
   */
  public CommitReportHandler() {
    this.builder = CommitReportData.newBuilder();
    this.fileNameToFileMetricHandlerMap = new HashMap<>();
  }

  /**
   * Clears the commitReportData from old data entries. Gets called in
   * {@link CommitReportHandler#init(String, String, String)} automatically.
   */
  public void clear() {
    this.builder = CommitReportData.newBuilder();
    this.fileNameToFileMetricHandlerMap.clear(); /* TODO: unmodified files 
                                                  * should be kept for a better performance*/
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
    this.fileNameToFileMetricHandlerMap.put(fileDescriptor.relativePath, new FileMetricHandler());
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

  public void addFileHash(final FileDescriptor fileDescriptor) {
    String s = fileDescriptor.objectId.toString();
    String[] sa = s.split("\\[");
    s = sa[1].substring(0, sa[1].length() - 1);
    builder.addFileHash(s);
  }

  public void addModified(final FileDescriptor fileDescriptor) {
    builder.addModified(fileDescriptor.relativePath);
  }

  public void addDeleted(final FileDescriptor fileDescriptor) {
    builder.addDeleted(fileDescriptor.relativePath);
  }

  public void addAdded(final FileDescriptor fileDescriptor) {
    builder.addAdded(fileDescriptor.relativePath);
  }


  public FileMetricHandler getFileMetricHandler(final String fileName) {
    return this.fileNameToFileMetricHandlerMap.get(fileName);
  }

  /**
   * ... 
   */
  public void addTags(final List<String> tags) {
    for (final String tag : tags) {
      builder.addTags(tag);
    }
  }

  public void addToken(final String token) {
    builder.setLandscapeToken(token);
  }

  public void addApplicationName(final String applicationName) {
    builder.setApplicationName(applicationName);
  }

  /**
  * Sets the lines of code (loc) metric.
  ** @param fileDescriptor the file descriptor of the corresponding file we want
  *     to set the lines of code for
  ** @param loc the lines of code
  */
  public void setLoc(final FileDescriptor fileDescriptor, final int loc) {
    final FileMetricHandler fileMetricHandler = this.fileNameToFileMetricHandlerMap
        .get(fileDescriptor.relativePath);
    fileMetricHandler.setLoc(loc);
  }

  /**
  * Sets the cyclomatic complexity metric. 
  ** @param fileDescriptor the file descriptor of the corresponding file we want
  *     to set the cyclomatic complexity for 
  * * @param cyclomaticComplexity the cyclomatic complexity
  */
  public void setCyclomaticComplexity(final FileDescriptor fileDescriptor, 
      final int cyclomaticComplexity) {
    final FileMetricHandler fileMetricHandler = this.fileNameToFileMetricHandlerMap
        .get(fileDescriptor.relativePath);
    fileMetricHandler.setCyclomaticComplexity(cyclomaticComplexity);
  }

  /**
  * Sets the number of methods metric.
  *
  * @param fileDescriptor the file descriptor of the corresponding file we want
  *     to set the number of methods for
  * @param numberOfMethods the number of methods
  */
  public void setNumberOfMethods(final FileDescriptor fileDescriptor, 
      final int numberOfMethods) {
    final FileMetricHandler fileMetricHandler = this.fileNameToFileMetricHandlerMap
        .get(fileDescriptor.relativePath);
    fileMetricHandler.setNumberOfMethods(numberOfMethods);
  }

  /** Returns the commit report data.
   ** 
   ** @return commit report data object
   */
  public CommitReportData getCommitReport() {
    for (final Map.Entry<String, FileMetricHandler> entry : this.fileNameToFileMetricHandlerMap
        .entrySet()) {
      if (entry.getValue().getFileName() != "") { // NOPMD
        this.builder.addFileMetric(entry.getValue()
            .getProtoBufObject()); // only add FileMetrics that do have metric data
      }
    }
    return builder.build();
  }
}
