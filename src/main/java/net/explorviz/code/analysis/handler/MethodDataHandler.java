package net.explorviz.code.analysis.handler;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import java.util.List;
import java.util.Map;
import net.explorviz.code.proto.MethodData;
import net.explorviz.code.proto.ParameterData;

/**
 * MethodData object holds data from analyzed method.
 */
public class MethodDataHandler implements ProtoBufConvertable<MethodData> {
  private static final int STRING_BUILDER_CAPACITY = 300;

  private final MethodData.Builder builder;


  /**
   * Creates a new MethodData object holding data describing the method.
   *
   * @param returnType the return type of the method
   */
  public MethodDataHandler(final String returnType) {
    this.builder = MethodData.newBuilder();
    this.builder.setIsConstructor(false);
    this.builder.setReturnType(returnType);
  }

  /**
   * Creates a new Constructor object holding data describing the method.
   */
  public MethodDataHandler() {
    this.builder = MethodData.newBuilder();
    this.builder.setIsConstructor(true);
  }

  public void addModifier(final String modifier) {
    this.builder.addModifier(modifier);
  }

  public void addAnnotation(final String annotation) {
    this.builder.addAnnotation(annotation);
  }

  /**
   * Add a parameter to the method.
   *
   * @param name name of the parameter
   * @param type type of the parameter
   * @param modifiers modifiers of the parameter
   */
  public void addParameter(final String name, final String type,
                           final NodeList<Modifier> modifiers) {
    final ParameterData.Builder parameterBuilder = ParameterData.newBuilder();
    parameterBuilder.setName(name);
    parameterBuilder.setType(type);
    for (final Modifier mod : modifiers) {
      parameterBuilder.addModifier(mod.toString());
    }
    builder.addParameter(parameterBuilder);
  }

  /**
   * Add a parameter to the method.
   *
   * @param name name of the parameter
   * @param type type of the parameter
   * @param modifiers modifiers of the parameter
   */
  public void addParameter(final String name, final String type, final List<String> modifiers) {
    final ParameterData.Builder parameterBuilder = ParameterData.newBuilder();
    parameterBuilder.setName(name);
    parameterBuilder.setType(type);
    parameterBuilder.addAllModifier(modifiers);
    this.builder.addParameter(parameterBuilder);
  }

  public void addOutgoingMethodCall(final String fqn) {
    this.builder.addOutgoingMethodCalls(fqn);
  }

  /**
   * Adds a metric to the Method.
   *
   * @param metricName the name of the metric
   * @param metricValue the value of the metric
   * @return the old metric value if it existed, null otherwise
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
  public MethodData getProtoBufObject() {
    return this.builder.build();
  }

  @Override
  public String toString() {
    final StringBuilder metricDataString = new StringBuilder(STRING_BUILDER_CAPACITY);
    for (final Map.Entry<String, String> entry : this.builder.getMetricMap().entrySet()) {
      metricDataString.append(entry.getKey()).append(": ");
      metricDataString.append(entry.getValue()).append('\n');
    }
    return "  type: " + this.builder.getReturnType() + "\n" + "  modifiers: "
        + this.builder.getModifierList() + "\n" + "  parameters: " + this.builder.getParameterList()
        + "\n" + "  outgoing calls: " + this.builder.getOutgoingMethodCallsList() + "\n"
        + metricDataString;
  }
}
