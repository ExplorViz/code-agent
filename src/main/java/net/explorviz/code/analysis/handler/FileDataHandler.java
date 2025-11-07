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

  private static final int MIN_STACK_SIZE_FOR_PARENT = 2;
  private static final int STRING_BUILDER_CAPACITY = 300;
  // classStack is an internal object to keep track of the class hierarchy
  private final Stack<String> classStack;
  private final Stack<String> methodStack;

  private final FileData.Builder builder;
  private final Map<String, ClassDataHandler> classDataMap;

  /**
   * Creates a blank FileData object.
   */
  public FileDataHandler(final String fileName) {
    this.builder = FileData.newBuilder().setFileName(fileName);
    this.classStack = new Stack<>();
    this.methodStack = new Stack<>();
    this.classDataMap = new HashMap<>();
  }

  /**
   * Enters a class, used while walking the AST.
   *
   * @param className the name of the entered class
   */
  public void enterClass(final String className) {
    this.classStack.push(className);
    this.classDataMap.put(className, new ClassDataHandler());

    if (this.classStack.size() >= MIN_STACK_SIZE_FOR_PARENT) {
      final String parentClassName =
          this.classStack.get(this.classStack.size() - MIN_STACK_SIZE_FOR_PARENT);
      final ClassDataHandler parent = this.getClassData(parentClassName);
      if (parent != null) { // Add null check for robustness
        parent.addInnerClass(className);
      }
    }

  }

  /**
   * Enters an anonymous class, used while walking the AST.
   *
   * @param anonymousClassName the type of the anonymous class, as no real name is available
   * @param parentFqn          the fqn of the parent, may be the method it is created in.
   */
  public void enterAnonymousClass(final String anonymousClassName, final String parentFqn) {
    String baseFqn = parentFqn + "." + anonymousClassName;
    String fqn = baseFqn;
    int idx = 0;
    while (classDataMap.containsKey(fqn)) {
      idx++;
      fqn = String.format("%s#%d", baseFqn, idx);
    }

    this.classStack.push(fqn);
    final ClassDataHandler classDataHandler = new ClassDataHandler();
    classDataHandler.setIsAnonymousClass();
    this.classDataMap.put(fqn, classDataHandler);
  }


  /**
   * Adds a new metric entry to the FileData, returns the old value of the metric if it existed,
   * null otherwise.
   *
   * @param metricName  the name/idetifier of the metric
   * @param metricValue the value of the metric
   * @return the old value of the metric if it existed, null otherwise.
   */
  public String addMetric(final String metricName, final String metricValue) {
    final String oldMetricValue = builder.getMetricOrDefault(metricName, null);
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
   * Returns the metrics map.
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

  /**
   * Returns the full qualified name of the class that is on top of the class stack.
   *
   * @return fqn of class on top of stack
   */
  public String getCurrentClassFqn() {
    if (classStack.isEmpty()) {
      throw new IllegalStateException("Class stack is empty. Cannot get current class FQN.");
    }
    return this.classStack.peek();
  }

  public ClassDataHandler getClassData(final String className) {
    return this.classDataMap.get(className);
  }

  public ClassDataHandler getCurrentClassData() {
    return this.classDataMap.get(getCurrentClassFqn());
  }

  /**
   * Removes class from top of the class stack (pop action).
   */
  public void leaveClass() {
    if (classStack.isEmpty()) {
      throw new IllegalStateException("Class stack is empty. Cannot pop current class FQN.");
    }
    this.classStack.pop();
  }

  public void leaveAnonymousClass() {
    leaveClass();
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

  /**
   * Set the SHA-1 of the current commit.
   *
   * @param commitSha the SHA-1 of the commit.
   */
  public void setCommitSha(final String commitSha) {
    this.builder.setCommitID(commitSha);
  }

  public String getFileName() {
    return this.builder.getFileName();
  }

  /**
   * Set the modifications entries.
   *
   * @param modifiedLines the amount of modified lines in this file
   * @param addedLines    the amount of added lines in this file
   * @param deletedLines  the amount of deleted line in this file
   */
  public void setModifications(final int modifiedLines, final int addedLines,
      final int deletedLines) {
    this.builder.setModifiedLines(Integer.toString(modifiedLines));
    this.builder.setAddedLines(Integer.toString(addedLines));
    this.builder.setDeletedLines(Integer.toString(deletedLines));
  }

  public void setAuthor(final String author) {
    this.builder.setAuthor(author);
  }

  public void setLandscapeToken(final String landscapeToken) {
    this.builder.setLandscapeToken(landscapeToken);
  }

  public void setApplicationName(final String applicationName) {
    this.builder.setApplicationName(applicationName);
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
    return "stats: methodCount=" + this.getMethodCount() + "\n" + "package: "
        + this.builder.getPackageName() + "\n" + "imports: " + this.builder.getImportNameList()
        + "\n" + mapData;
  }

  public String getCurrentMethodFqn() {
    return methodStack.peek();
  }

  public void enterMethod(final String methodFqn) {
    methodStack.push(methodFqn);
  }

  public void leaveMethod() {
    methodStack.pop();
  }
}
