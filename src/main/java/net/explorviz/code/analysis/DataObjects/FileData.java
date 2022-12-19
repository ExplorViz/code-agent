package net.explorviz.code.analysis.DataObjects;

import java.util.*;

public class FileData {
  private Stack<String> classStack;

  public String packageName;
  public List<String> importNames;
  public HashMap<String, ClassData> classDataHashMap;


  public FileData() {
    this.classStack = new Stack<>();
    this.importNames = new ArrayList<>();
    this.classDataHashMap = new HashMap<>();

  }

  public void enterClass(String className) {
    this.classStack.push(className);
    this.classDataHashMap.put(className, new ClassData());
  }

  public String getCurrentClassName() {
    return this.classStack.lastElement();
  }

  public ClassData getCurrentClassData() {
    return this.classDataHashMap.get(getCurrentClassName());
  }

  public void leaveClass() {
    this.classStack.pop();
  }

  public int getMethodCount() {
    int methodCount = 0;
    for (Map.Entry<String, ClassData> entry : this.classDataHashMap.entrySet()) {
      methodCount += entry.getValue().getMethodCount(true);
    }
    return methodCount;
  }

  @Override
  public String toString() {
    StringBuilder classDataString = new StringBuilder(300);
    for (Map.Entry<String, ClassData> entry : this.classDataHashMap.entrySet()) {
      classDataString.append(entry.getKey()).append(": \n");
      classDataString.append(entry.getValue().toString());
      classDataString.append("\n");
    }
    return "stats: methodCount=" + this.getMethodCount() + "\n"
        + "package: " + this.packageName + "\n"
        + "imports: " + importNames.toString() + "\n"
        + classDataString;
  }
}
