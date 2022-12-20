package net.explorviz.code.analysis.dataobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * FileData object holds data from a analyzed .java file.
 */
public class FileData {
  private static final int STRING_BUILDER_CAPACITY = 300;
  private final Stack<String> classStack;

  private int loc;
  private String packageName;
  private final List<String> importNames;
  private final Map<String, ClassData> classDataMap;


  /**
   * Creates a blank FileData object.
   */
  public FileData() {
    this.classStack = new Stack<>();
    this.importNames = new ArrayList<>();
    this.classDataMap = new HashMap<>();
  }

  /**
   * Enters a Class, used while walking the AST.
   *
   * @param className the name of the entered class
   */
  public void enterClass(final String className) {
    this.classStack.push(className);
    this.classDataMap.put(className, new ClassData());

    // check if stack has at least 2 entries
    if (this.classStack.size() > 1) { // NOPMD
      // get the penultimate class object, that's the current's parent
      final ClassData parent = this.getClassData(this.classStack.get(this.classStack.size() - 2));
      parent.addInnerClass(className);
    }
  }

  public void setLoc(final int loc) {
    this.loc = loc;
  }

  public void addImport(final String importName) {
    this.importNames.add(importName);
  }

  public List<String> getImportNames() {
    return this.importNames;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName;
  }

  public String getCurrentClassName() {
    return this.classStack.lastElement();
  }

  private ClassData getClassData(final String className) {
    return this.classDataMap.get(className);
  }

  public ClassData getCurrentClassData() {
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
    for (final Map.Entry<String, ClassData> entry : this.classDataMap.entrySet()) {
      methodCount += entry.getValue().getMethodCount();
    }
    return methodCount;
  }

  @Override
  public String toString() {
    final StringBuilder classDataString = new StringBuilder(STRING_BUILDER_CAPACITY);
    for (final Map.Entry<String, ClassData> entry : this.classDataMap.entrySet()) {
      classDataString.append(entry.getKey()).append(": \n");
      classDataString.append(entry.getValue().toString());
      classDataString.append('\n');
    }
    return "stats: methodCount=" + this.getMethodCount() + "  loc=" + this.loc + "\n"
        + "package: " + this.packageName + "\n"
        + "imports: " + importNames.toString() + "\n"
        + classDataString;
  }
}
