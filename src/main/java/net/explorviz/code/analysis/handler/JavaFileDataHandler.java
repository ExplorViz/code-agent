package net.explorviz.code.analysis.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.Language;

/**
 * FileData handler for Java files.
 */
public class JavaFileDataHandler extends AbstractFileDataHandler
    implements ProtoBufConvertable<FileData> {

  private static final int MIN_STACK_SIZE_FOR_PARENT = 2;
  private static final int STRING_BUILDER_CAPACITY = 300;
  
  private final Stack<String> classStack;
  private final Stack<String> methodStack;
  private final Map<String, ClassDataHandler> classDataMap;

  public JavaFileDataHandler(final String fileName) {
    super(fileName);
    this.classStack = new Stack<>();
    this.methodStack = new Stack<>();
    this.classDataMap = new HashMap<>();
  }

  public void enterClass(final String className) {
    this.classStack.push(className);
    this.classDataMap.put(className, new ClassDataHandler());

    if (this.classStack.size() >= MIN_STACK_SIZE_FOR_PARENT) {
      final String parentClassName =
          this.classStack.get(this.classStack.size() - MIN_STACK_SIZE_FOR_PARENT);
      final ClassDataHandler parent = this.getClassData(parentClassName);
      if (parent != null) {
        parent.addInnerClass(className);
      }
    }
  }

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

  public void leaveClass() {
    if (classStack.isEmpty()) {
      throw new IllegalStateException("Class stack is empty. Cannot pop current class FQN.");
    }
    this.classStack.pop();
  }

  public void leaveAnonymousClass() {
    leaveClass();
  }

  public int getMethodCount() {
    int methodCount = 0;
    for (final Map.Entry<String, ClassDataHandler> entry : this.classDataMap.entrySet()) {
      methodCount += entry.getValue().getMethodCount();
    }
    return methodCount;
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

  public Map<String, String> getMetrics() {
    return builder.getMetricMap();
  }

  public List<String> getImportNames() {
    return this.builder.getImportNameList();
  }

  @Override
  public FileData getProtoBufObject() {
    // Add all class data to builder
    for (final Map.Entry<String, ClassDataHandler> entry : this.classDataMap.entrySet()) {
      this.builder.putClassData(entry.getKey(), entry.getValue().getProtoBufObject());
    }
    
    // Set language
    builder.setLanguage(Language.JAVA);
    
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
}

