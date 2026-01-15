package net.explorviz.code.analysis.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.explorviz.code.proto.ExportData;
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

  private final List<FunctionData.Builder> globalFunctions;

  private final List<ExportData.Builder> exports;

  private boolean inClassContext = false;

  public TypeScriptFileDataHandler(final String fileName) {
    super(fileName);
    this.classStack = new Stack<>();
    this.classDataMap = new HashMap<>();
    this.globalFunctions = new ArrayList<>();
    this.exports = new ArrayList<>();
  }

  public void enterClass(final String className) {
    final String classFqn = fileName + ":" + className;
    this.classStack.push(classFqn);
    this.classDataMap.put(classFqn, new ClassDataHandler());
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

  public FunctionData.Builder addGlobalFunction(final String name, final String returnType) {
    final String functionFqn = fileName + ":" + name;
    final FunctionData.Builder funcBuilder = FunctionData.newBuilder()
        .setName(name)
        .setFqn(functionFqn)
        .setReturnType(returnType);

    globalFunctions.add(funcBuilder);
    return funcBuilder;
  }

  public void addExport(final String name, final String type, final boolean isDefault) {
    final ExportData.Builder exportBuilder = ExportData.newBuilder()
        .setName(name)
        .setType(type)
        .setIsDefault(isDefault);

    exports.add(exportBuilder);
  }

  public int getGlobalFunctionCount() {
    return globalFunctions.size();
  }

  public int getClassCount() {
    return classDataMap.size();
  }

  public int getTotalFunctionCount() {
    int count = globalFunctions.size();
    for (final ClassDataHandler classData : classDataMap.values()) {
      count += classData.getMethodCount();
    }
    return count;
  }

  @Override
  public FileData getProtoBufObject() {
    for (final Map.Entry<String, ClassDataHandler> entry : classDataMap.entrySet()) {
      builder.putClassData(entry.getKey(), entry.getValue().getProtoBufObject());
    }

    for (final FunctionData.Builder funcBuilder : globalFunctions) {
      builder.addFunctions(funcBuilder.build());
    }

    for (final ExportData.Builder exportBuilder : exports) {
      builder.addExports(exportBuilder.build());
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
    result.append("Imports: ").append(builder.getImportNameList()).append("\n");
    result.append("Classes: ").append(classDataMap.size()).append("\n");
    result.append("Global Functions: ").append(globalFunctions.size()).append("\n");
    result.append("Exports: ").append(exports.size()).append("\n");

    for (final Map.Entry<String, ClassDataHandler> entry : classDataMap.entrySet()) {
      result.append("  Class ").append(entry.getKey()).append(":\n");
      result.append(entry.getValue().toString()).append("\n");
    }

    if (!globalFunctions.isEmpty()) {
      result.append("Global Functions:\n");
      for (final FunctionData.Builder func : globalFunctions) {
        result.append("  - ").append(func.getName()).append("\n");
      }
    }

    for (final Map.Entry<String, String> entry : builder.getMetricMap().entrySet()) {
      result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
    }

    return result.toString();
  }
}

