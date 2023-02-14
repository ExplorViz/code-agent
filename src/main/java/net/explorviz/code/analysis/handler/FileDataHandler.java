package net.explorviz.code.analysis.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.explorviz.code.proto.FileData;

/**
 * FileData object holds data from a analyzed .java file.
 */
public class FileDataHandler implements ProtoBufConvertable<FileData> {

  private static final int STRING_BUILDER_CAPACITY = 300;
  // classStack is an internal object to keep track of the class hierarchy
  private final Stack<String> classStack;

  private final FileData.Builder builder;
  private final Map<String, ClassDataHandler> classDataMap;

  /**
   * Creates a blank FileData object.
   */
  public FileDataHandler(final String fileName) {
    this.builder = FileData.newBuilder();
    this.builder.setFileName(fileName);
    this.classStack = new Stack<>();
    this.classDataMap = new HashMap<>();
  }

  /**
   * Enters a Class, used while walking the AST.
   *
   * @param className the name of the entered class
   */
  public void enterClass(final String className) {
    this.classStack.push(className);
    this.classDataMap.put(className, new ClassDataHandler());

    // check if stack has at least 2 entries
    if (this.classStack.size() > 1) { // NOPMD
      // get the penultimate class object, that's the current's parent
      final ClassDataHandler parent = this.getClassData(
          this.classStack.get(this.classStack.size() - 2));
      parent.addInnerClass(className);
    }
  }

  /**
   * Adds a new metric entry to the FileData, returns the old value of the metric if it existed,
   * null otherwise.
   *
   * @param metricName the name/idetifier of the metric
   * @param metricValue the value of the metric
   * @return the old value of the metric if it existed, null otherwise.
   */
  public String addMetric(final String metricName, final String metricValue) {
    String oldMetricValue = builder.getMetricOrDefault(metricName, null);
    builder.putMetric(metricName, metricValue);
    return oldMetricValue;
  }

  /**
   * Returns the value of the metric, if no entry with the name exists, returns null.
   *
   * @param metricName the name/identifier of the metric
   * @return the value of the metric or null if the metric does not exist
   */
  public String getMetricValue(final String metricName) {
    return builder.getMetricOrDefault(metricName, null);
  }

  /**
   * Returns the metrics map
   *
   * @return the map containing the File metrics
   */
  public Map<String, String> getMetrics() {
    return builder.getMetricMap();
  }

  public void addImport(final String importName) {
    this.builder.addImportName(importName);
  }

  public List<String> getImportNames() {
    return this.builder.getImportNameList();
  }

  public void setPackageName(final String packageName) {
    this.builder.setPackageName(packageName);
  }

  public String getCurrentClassName() {
    return this.classStack.lastElement();
  }

  public ClassDataHandler getClassData(final String className) {
    return this.classDataMap.get(className);
  }

  public ClassDataHandler getCurrentClassData() {
    return this.classDataMap.get(getCurrentClassName());
  }

  public void leaveClass() {
    this.classStack.pop();
  }

  /**
   * Calculates the number of methods in this file.
   *
   * @return the number of methods
   */
  public int getMethodCount() {
    int methodCount = 0;
    for (final Map.Entry<String, ClassDataHandler> entry : this.classDataMap.entrySet()) {
      methodCount += entry.getValue().getMethodCount();
    }
    return methodCount;
  }

  public String getFileName() {
    return this.builder.getFileName();
  }

  @Override
  public FileData getProtoBufObject() {
    for (final Map.Entry<String, ClassDataHandler> entry : this.classDataMap.entrySet()) {
      this.builder.putClassData(entry.getKey(), entry.getValue().getProtoBufObject());
    }
    return builder.build();
  }

  @Override
  public String toString() {
    final StringBuilder mapData = new StringBuilder(STRING_BUILDER_CAPACITY);
    for (final Map.Entry<String, ClassDataHandler> entry : this.classDataMap.entrySet()) {
      mapData.append(entry.getKey()).append(": \n");
      mapData.append(entry.getValue().toString());
      mapData.append('\n');
    }
    for (final Map.Entry<String, String> entry : this.builder.getMetricMap().entrySet()) {
      mapData.append(entry.getKey()).append(": ");
      mapData.append(entry.getValue()).append('\n');
    }
    return "stats: methodCount=" + this.getMethodCount() + "\n"
        + "package: " + this.builder.getPackageName() + "\n"
        + "imports: " + this.builder.getImportNameList() + "\n"
        + mapData;
  }
}
