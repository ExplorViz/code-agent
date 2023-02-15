package net.explorviz.code.analysis.handler;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * The MetricAppender is a helper Object to facilitate the adding of metric data to the internal
 * FileData object. It provides basic functionality to keep track of the class hierarchy and handles
 * the adding of metrics to the right objects.
 */
public class MetricAppender {

  private static final String UNKNOWN = "UNKNOWN";
  private final FileDataHandler fileData;
  private final Stack<String> classStack;
  private final Stack<String> methodStack;

  public MetricAppender(FileDataHandler fileDataHandler) {
    this.fileData = fileDataHandler;
    this.classStack = new Stack<>();
    this.methodStack = new Stack<>();
  }

  /**
   * Sets the metric with metricName to metricValue. The metric is attached to the current method
   * set by {@link #enterMethod(MethodDeclaration)}
   *
   * @param metricName the name of the metric
   * @param metricValue the value of the metric
   * @return returns the old value of the metric if the metric exists, otherwise null
   */
  public String putMethodMetric(String metricName, String metricValue) {
    // TODO catch if class or method does not exist
    ClassDataHandler classDataHandler = fileData.getClassData(classStack.peek());
    MethodDataHandler methodDataHandler = classDataHandler.getMethod(methodStack.peek());
    return methodDataHandler.addMetric(metricName, metricValue);
    // return fileData.getClassData(classStack.peek()).getMethod(methodName).addMetric(metricName, metricValue);
  }

  /**
   * Sets the metric with metricName to metricValue. The metric is attached to the current class set
   * by {@link #enterClass(ClassOrInterfaceDeclaration)}
   *
   * @param metricName the name of the metric
   * @param metricValue the value of the metric
   * @return returns the old value of the metric if the metric exists, otherwise null
   */
  public String putClassMetric(String metricName, String metricValue) {
    // TODO catch if class data not available
    ClassDataHandler classDataHandler = fileData.getClassData(classStack.peek());
    return classDataHandler.addMetric(metricName, metricValue);
  }

  public String putMethodMetric(String metricName, String metricValue,
                                MethodDeclaration method) {
    // TODO seem odd like this, maybe clean this or remove completely
    if (method.hasParentNode()) {
      // method.getParentNode().get().
      Node parent = method.getParentNode().get();
      if (parent instanceof ClassOrInterfaceDeclaration) {
        String fqn = ((ClassOrInterfaceDeclaration) parent).getFullyQualifiedName().orElse(UNKNOWN);
        return fileData.getClassData(fqn)
            .getMethod(fqn + "." + method.getNameAsString() + "#" + parameterHash(
                method.getParameters())).addMetric(metricName, metricValue);
      }
    }
    return null;
  }

  public String putMethodMetric(String metricName, String metricValue, String classFqn,
                                String methodFqn) {
    return fileData.getClassData(classFqn).getMethod(methodFqn).addMetric(metricName, metricValue);
  }

  public String putClassMetric(String metricName, String metricValue,
                               ClassOrInterfaceDeclaration clazz) {
    return fileData.getClassData(clazz.getFullyQualifiedName().orElse(UNKNOWN))
        .addMetric(metricName, metricValue);
  }

  public String putClassMetric(String metricName, String metricValue, String classFqn) {
    return fileData.getClassData(classFqn).addMetric(metricName, metricValue);
  }

  /**
   * Sets the metric with metricName to metricValue. The metric is attached to the current file.
   *
   * @param metricName the name of the metric
   * @param metricValue the value of the metric
   * @return returns the old value of the metric if the metric exists, otherwise null
   */
  public String putFileMetric(String metricName, String metricValue) {
    return fileData.addMetric(metricName, metricValue);
  }

  public void enterClass(final ClassOrInterfaceDeclaration clazz) {
    this.classStack.push(clazz.getFullyQualifiedName().orElse(UNKNOWN));
  }

  public void leaveClass() {
    this.classStack.pop();
  }

  // TODO Name or FQN?
  public String getCurrentClassName() {
    return classStack.peek();
  }

  public void enterMethod(final MethodDeclaration method) {
    methodStack.push(getCurrentClassName() + "." + method.getNameAsString() + "#" + parameterHash(
        method.getParameters()));
  }

  public void leaveMethod() {
    methodStack.pop();
  }

  // TODO Name or FQN?
  public String getCurrentMethodName() {
    return methodStack.peek();
  }

  public FileDataHandler getFileData() {
    return fileData;
  }

  /**
   * Calculates the hash for a parameter list provided as String List.
   *
   * @param list a list of Types
   * @return the hash of the types as hexadecimal string
   */
  public static String parameterHash(final List<String> list) {
    return Integer.toHexString(list.hashCode());
  }

  /**
   * Calculates the hash for a parameter list provided as {@link NodeList}.
   *
   * @param parameterList a list of Parameters
   * @return the hash of the parameters as hexadecimal string
   */
  public static String parameterHash(final NodeList<Parameter> parameterList) {
    final List<String> tempList = new ArrayList<>();
    for (final Parameter parameter : parameterList) {
      tempList.add(parameter.getName().asString());
    }
    return Integer.toHexString(tempList.hashCode());
  }
}
