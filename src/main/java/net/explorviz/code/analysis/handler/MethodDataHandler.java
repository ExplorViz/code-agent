package net.explorviz.code.analysis.handler;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import java.util.List;
import net.explorviz.code.proto.MethodData;
import net.explorviz.code.proto.ParameterData;

/**
 * MethodData object holds data from analyzed method.
 */
public class MethodDataHandler implements ProtoBufConvertable<MethodData> {

  private final MethodData.Builder builder;


  /**
   * Creates a new MethodData object holding data describing the method.
   *
   * @param returnType the return type of the method
   */
  public MethodDataHandler(final String returnType) {
    this.builder = MethodData.newBuilder();
    this.builder.setReturnType(returnType);
  }

  public void addModifier(final String modifier) {
    this.builder.addModifier(modifier);
  }

  public void addParameter(final String name, final String type,
                           final NodeList<Modifier> modifiers) {
    ParameterData.Builder parameterBuilder = ParameterData.newBuilder();
    parameterBuilder.setName(name);
    parameterBuilder.setType(type);
    for (Modifier mod : modifiers) {
      parameterBuilder.addModifier(mod.toString());
    }
    builder.addParameter(parameterBuilder);
  }

  public void addParameter(final String name, final String type, final List<String> modifiers) {
    ParameterData.Builder parameterBuilder = ParameterData.newBuilder();
    parameterBuilder.setName(name);
    parameterBuilder.setType(type);
    parameterBuilder.addAllModifier(modifiers);
    this.builder.addParameter(parameterBuilder);
  }

  public void addOutgoingMethodCall(final String fqn) {
    this.builder.addOutgoingMethodCalls(fqn);
  }

  public void setLoc(final int loc) {
    this.builder.setLoc(loc);
  }

  @Override
  public MethodData getProtoBufObject() {
    return this.builder.build();
  }

  @Override
  public String toString() {
    return "  type: " + this.builder.getReturnType() + "\n"
        + "  modifiers: " + this.builder.getModifierList() + "\n"
        + "  parameters: " + this.builder.getParameterList() + "\n"
        + "  outgoing calls: " + this.builder.getOutgoingMethodCallsList();
  }
}
