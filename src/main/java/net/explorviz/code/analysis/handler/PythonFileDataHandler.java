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
 * File data handler specifically for Python files.
 */
public class PythonFileDataHandler extends AbstractFileDataHandler {

  private final Stack<String> classStack = new Stack<>();
  private final Map<String, ClassDataHandler> classDataMap = new HashMap<>();
  private final Stack<String> methodStack = new Stack<>();
  private final List<FunctionData.Builder> globalFunctions = new ArrayList<>();

  public PythonFileDataHandler(final String fileName) {
    super(fileName);
    builder.setLanguage(Language.PYTHON);
  }

  public void enterClass(final String className) {
    this.classStack.push(className);
    this.classDataMap.put(className, new ClassDataHandler());
  }

  public void leaveClass() {
    if (!classStack.isEmpty()) {
      this.classStack.pop();
    }
  }

  public void enterMethod(final String methodFqn) {
    methodStack.push(methodFqn);
  }

  public void leaveMethod() {
    if (!methodStack.isEmpty()) {
      methodStack.pop();
    }
  }

  public boolean isInClassContext() {
    return !classStack.isEmpty();
  }

  public ClassDataHandler getCurrentClassData() {
    if (classStack.isEmpty()) {
      return null;
    }
    return classDataMap.get(classStack.peek());
  }

  public String getCurrentClassFqn() {
    if (classStack.isEmpty()) {
      return null;
    }
    return classStack.peek();
  }

  public String getCurrentMethodFqn() {
    if (methodStack.isEmpty()) {
      return null;
    }
    return methodStack.peek();
  }

  public FunctionData.Builder addGlobalFunction(final String name, final String returnType) {
    final FunctionData.Builder funcBuilder = FunctionData.newBuilder()
        .setName(name)
        .setFqn(name)  // Simple FQN for now
        .setReturnType(returnType);

    globalFunctions.add(funcBuilder);
    return funcBuilder;
  }

  @Override
  public FileData getProtoBufObject() {
    // Add all classes
    for (final Map.Entry<String, ClassDataHandler> entry : classDataMap.entrySet()) {
      builder.putClassData(entry.getKey(), entry.getValue().getProtoBufObject());
    }
    
    // Add all global functions
    for (final FunctionData.Builder funcBuilder : globalFunctions) {
      builder.addFunctions(funcBuilder.build());
    }

    return builder.build();
  }
}
