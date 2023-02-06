package net.explorviz.code.analysis.handler;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import java.util.List;
import net.explorviz.code.proto.ConstructorData;
import net.explorviz.code.proto.ParameterData;

public class ConstructorDataHandler implements ProtoBufConvertable<ConstructorData> {

  private final ConstructorData.Builder builder;

  /**
   * Creates a new ConstructorData object holding data describing the constructor.
   */
  public ConstructorDataHandler() {
    this.builder = ConstructorData.newBuilder();

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
  public ConstructorData getProtoBufObject() {
    return this.builder.build();
  }

  @Override
  public String toString() {
    return "  modifiers: " + this.builder.getModifierList() + "\n"
        + "  parameters: " + this.builder.getParameterList() + "\n"
        + "  outgoing calls: " + this.builder.getOutgoingMethodCallsList();
  }
}