package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.FileData;

public abstract class AbstractFileDataHandler {

  protected final FileData.Builder builder;
  protected final String fileName;

  protected AbstractFileDataHandler(final String fileName) {
    this.fileName = fileName;
    this.builder = FileData.newBuilder().setFileName(fileName);
  }

  public String getFileName() {
    return fileName;
  }

  public void setCommitSha(final String commitSha) {
    builder.setCommitID(commitSha);
  }

  public void setPackageName(final String packageName) {
    builder.setPackageName(packageName);
  }

  public void addImport(final String importName) {
    builder.addImportName(importName);
  }

  public String addMetric(final String metricName, final String metricValue) {
    builder.putMetric(metricName, metricValue);
    return metricValue;
  }

  public String getMetricValue(final String metricName) {
    return builder.getMetricOrDefault(metricName, null);
  }

  public void setModifications(final int modifiedLines, final int addedLines,
      final int deletedLines) {
    builder.setModifiedLines(String.valueOf(modifiedLines));
    builder.setAddedLines(String.valueOf(addedLines));
    builder.setDeletedLines(String.valueOf(deletedLines));
  }

  public void setAuthor(final String author) {
    builder.setAuthor(author);
  }

  public void setLandscapeToken(final String landscapeToken) {
    builder.setLandscapeToken(landscapeToken);
  }

  public void setApplicationName(final String applicationName) {
    builder.setApplicationName(applicationName);
  }

  public abstract FileData getProtoBufObject();

  @Override
  public String toString() {
    return getProtoBufObject().toString();
  }
}

