package net.explorviz.code.analysis.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.explorviz.code.proto.ClassData;
import net.explorviz.code.proto.ClassType;
import net.explorviz.code.proto.FieldData;

/**
 * ClassData object holds data from analyzed classes.
 */
public class ClassDataHandler implements ProtoBufConvertable<ClassData> {

  private static final int STRING_BUILDER_CAPACITY = 300;

  private final ClassData.Builder builder;

  private final Map<String, MethodDataHandler> methodDataMap;

  // private int loc;

  /**
   * Creates a blank ClassData object.
   */
  public ClassDataHandler() {
    this.builder = ClassData.newBuilder();
    this.builder.setSuperClass("");
    this.methodDataMap = new HashMap<>();
  }

  /**
   * Adds a methodData object.
   *
   * @param methodFqn  the method's fully qualified name
   * @param returnType the return type of the method
   * @return the created methodData object
   */
  public MethodDataHandler addMethod(final String methodFqn, final String returnType) {
    this.methodDataMap.put(methodFqn, new MethodDataHandler(returnType));
    return methodDataMap.get(methodFqn);
  }

  public MethodDataHandler getMethod(final String methodFqn) {
    return methodDataMap.get(methodFqn);
  }

  public void setSuperClass(final String superClass) {
    this.builder.setSuperClass(superClass);
  }

  public MethodDataHandler addConstructor(final String constructorFqn) {
    this.methodDataMap.put(constructorFqn, new MethodDataHandler());
    return methodDataMap.get(constructorFqn);
  }

  public void addField(final String fieldName, final String fieldType,
      final List<String> modifiers) {
    this.builder.addField(
        FieldData.newBuilder().setName(fieldName).setType(fieldType).addAllModifier(modifiers));
  }

  public void addModifier(final String modifier) {
    this.builder.addModifier(modifier);
  }

  public void addAnnotation(final String annotation) {
    this.builder.addAnnotation(annotation);
  }

  public void addInnerClass(final String name) {
    this.builder.addInnerClass(name);
  }

  public void addEnumConstant(final String name) {
    this.builder.addEnumConstant(name);
  }

  public int getMethodCount() {
    return this.methodDataMap.size();
  }

  public void addImplementedInterface(final String implementedInterfaceName) {
    this.builder.addInterface(implementedInterfaceName);
  }

  public void setIsInterface() {
    this.builder.setType(ClassType.INTERFACE);
  }

  public boolean isInterface() {
    return this.builder.getType() == ClassType.INTERFACE;
  }

  public void setIsAbstractClass() {
    this.builder.setType(ClassType.ABSTRACT_CLASS);
  }

  public boolean isAbstractClass() {
    return this.builder.getType() == ClassType.ABSTRACT_CLASS;
  }

  /**
   * Set the current ClassType as class if it wasn't set to anonymous class already.
   */
  public void setIsClass() {
    if (this.builder.getType() != ClassType.ANONYMOUS_CLASS) {
      this.builder.setType(ClassType.CLASS);
    }
  }

  /**
   * Set the current ClassType as class. If override is true, any ClassType value prior to this call
   * gets overridden.
   *
   * @param override set true to force set the ClassType, if false, the current classType is checked
   *                 to not override if it is set as anonymous class.
   */
  public void setIsClass(final boolean override) {
    if (override) {
      this.builder.setType(ClassType.CLASS);
    } else {
      setIsClass();
    }
  }

  public boolean isClass() {
    return this.builder.getType() == ClassType.CLASS;
  }

  public void setIsEnum() {
    this.builder.setType(ClassType.ENUM);
  }

  public boolean isEnum() {
    return this.builder.getType() == ClassType.ENUM;
  }

  public void setIsAnonymousClass() {
    this.builder.setType(ClassType.ANONYMOUS_CLASS);
  }

  public boolean isAnonymousClass() {
    return this.builder.getType() == ClassType.ANONYMOUS_CLASS;
  }


  /**
   * Adds a new metric entry to the ClassData, returns the old value of the metric if it existed,
   * null otherwise.
   *
   * @param metricName  the name/identifier of the metric
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

  @Override
  public ClassData getProtoBufObject() {
    for (final Map.Entry<String, MethodDataHandler> entry : this.methodDataMap.entrySet()) {
      this.builder.putMethodData(entry.getKey(), entry.getValue().getProtoBufObject());
    }
    return this.builder.build();
  }

  @Override
  public String toString() {
    final StringBuilder methodDataString = new StringBuilder(STRING_BUILDER_CAPACITY);
    for (final Map.Entry<String, MethodDataHandler> entry : this.methodDataMap.entrySet()) {
      methodDataString.append(entry.getKey()).append(": \n");
      methodDataString.append(entry.getValue().toString());
      methodDataString.append('\n');
    }
    final StringBuilder metricDataString = new StringBuilder(STRING_BUILDER_CAPACITY);
    for (final Map.Entry<String, String> entry : this.builder.getMetricMap().entrySet()) {
      metricDataString.append(entry.getKey()).append(": ");
      metricDataString.append(entry.getValue()).append('\n');
    }
    return "{ \n"
        + "type: " + this.builder.getType().toString() + "\n"
        + "modifier: " + this.builder.getModifierList() + "\n"
        + "superClass: " + this.builder.getSuperClass() + "\n"
        + "interfaces: " + this.builder.getInterfaceList() + "\n"
        + "fields: " + this.builder.getFieldList() + "\n"
        + "innerClasses: " + this.builder.getInnerClassList() + "\n"
        + "constructors: " + this.builder.getConstructorList() + "\n"
        + "methods: \n" + methodDataString + "\n"
        + "variables: " + this.builder.getVariableList() + "\n"
        + metricDataString + "\n}";
  }


}
