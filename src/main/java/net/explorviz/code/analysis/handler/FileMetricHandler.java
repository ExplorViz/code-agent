package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.FileMetric;

/**
 * The FileMetricHandler is used to create file metrics.
 */

public class FileMetricHandler implements ProtoBufConvertable<FileMetric> {

  private final FileMetric.Builder builder;

  /**
   * Creates a blank FileMetric object.
   */
  public FileMetricHandler() {
    this.builder = FileMetric.newBuilder();
  }

  public String getFileName() {
    return this.builder.getFileName();
  }

  public void setFileName(final String fileName) {
    this.builder.setFileName(fileName);
  }

  public void setFileSize(final int fileSize) {
    this.builder.setFileSize(fileSize);
  }

  public void setLoc(final int loc) {
    this.builder.setLoc(loc);
  }

  public void setCloc(final int cloc) {
    this.builder.setCloc(cloc);
  }

  public void setCyclomaticComplexity(final int cyclomaticComplexity) {
    this.builder.setCyclomaticComplexity(cyclomaticComplexity);
  }

  public void setNumberOfMethods(final int numberOfMethods) {
    this.builder.setNumberOfMethods(numberOfMethods);
  }

  @Override
  public FileMetric getProtoBufObject() {
    return this.builder.build();
  }

}
