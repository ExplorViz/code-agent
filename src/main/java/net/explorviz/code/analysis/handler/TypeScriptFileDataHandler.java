package net.explorviz.code.analysis.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.FunctionData;
import net.explorviz.code.proto.Language;

/**
 * FileData handler for TypeScript and JavaScript files.
 */
public class TypeScriptFileDataHandler extends AbstractFileDataHandler
    implements ProtoBufConvertable<FileData> {

  private static final int STRING_BUILDER_CAPACITY = 300;

  private final Stack<String> classStack;
  private final Map<String, ClassDataHandler> classDataMap;

  private final List<MethodDataHandler> globalFunctionHandlers;
  private final List<String> rootClasses;

  private boolean inClassContext = false;

  public TypeScriptFileDataHandler(final String fileName) {
    super(fileName);
    this.classStack = new Stack<>();
    this.classDataMap = new HashMap<>();
    this.globalFunctionHandlers = new ArrayList<>();
    this.rootClasses = new ArrayList<>();
  }

  public void enterClass(final String className) {
    final String classFqn = fileName + ":" + className;
    final ClassDataHandler handler = new ClassDataHandler();
    handler.setName(className);
    this.classDataMap.put(classFqn, handler);

    if (this.classStack.isEmpty()) {
      this.rootClasses.add(classFqn);
    } else {
      final String parentClassFqn = this.classStack.peek();
      final ClassDataHandler parent = this.classDataMap.get(parentClassFqn);
      if (parent != null) {
        parent.addInnerClass(className, handler);
      }
    }

    this.classStack.push(classFqn);
    this.inClassContext = true;
  }

  public void leaveClass() {
    if (!classStack.isEmpty()) {
      this.classStack.pop();
    }
    this.inClassContext = !classStack.isEmpty();
  }

  public String getCurrentClassFqnOrNull() {
    return classStack.isEmpty() ? null : classStack.peek();
  }

  public ClassDataHandler getCurrentClassData() {
    final String fqn = getCurrentClassFqnOrNull();
    return fqn != null ? classDataMap.get(fqn) : null;
  }

  public boolean isInClassContext() {
    return inClassContext;
  }

  public MethodDataHandler addGlobalFunction(final String name, final String returnType) {
    final MethodDataHandler handler = new MethodDataHandler(name, returnType);
    globalFunctionHandlers.add(handler);
    return handler;
  }

  public int getGlobalFunctionCount() {
    return globalFunctionHandlers.size();
  }

  public int getClassCount() {
    return classDataMap.size();
  }

  public int getTotalFunctionCount() {
    int count = globalFunctionHandlers.size();
    for (final ClassDataHandler classData : classDataMap.values()) {
      count += classData.getMethodCount();
    }
    return count;
  }

  @Override
  public FileData getProtoBufObject() {
    builder.clearClasses();
    for (final String rootClassFqn : rootClasses) {
      final ClassDataHandler handler = classDataMap.get(rootClassFqn);
      if (handler != null) {
        builder.addClasses(handler.getProtoBufObject());
      }
    }

    builder.clearFunctions();
    for (final MethodDataHandler handler : globalFunctionHandlers) {
      builder.addFunctions(handler.getProtoBufObject());
    }

    final Language language = fileName.endsWith(".ts") || fileName.endsWith(".tsx")
        ? Language.TYPESCRIPT
        : Language.JAVASCRIPT;
    builder.setLanguage(language);

    return builder.build();
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(STRING_BUILDER_CAPACITY);

    result.append("Language: ")
        .append(fileName.endsWith(".ts") ? "TypeScript" : "JavaScript")
        .append("\n");
    result.append("Package/Module: ").append(builder.getPackageName()).append("\n");
    result.append("Imports: ").append(builder.getImportNamesList()).append("\n");
    result.append("Classes: ").append(classDataMap.size()).append("\n");
    result.append("Global Functions: ").append(globalFunctionHandlers.size()).append("\n");

    for (final Map.Entry<String, ClassDataHandler> entry : classDataMap.entrySet()) {
      result.append("  Class ").append(entry.getKey()).append(":\n");
      result.append(entry.getValue().toString()).append("\n");
    }

    if (!globalFunctionHandlers.isEmpty()) {
      result.append("Global Functions:\n");
      for (final MethodDataHandler handler : globalFunctionHandlers) {
        result.append("  - ").append(handler.getProtoBufObject().getName()).append("\n");
      }
    }

    for (final Map.Entry<String, Double> entry : builder.getMetricsMap().entrySet()) {
      result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    return result.toString();
  }
}
