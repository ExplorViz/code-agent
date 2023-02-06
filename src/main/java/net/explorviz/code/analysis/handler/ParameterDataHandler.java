package net.explorviz.code.analysis.handler;

import java.util.List;
import net.explorviz.code.proto.ParameterData;

public class ParameterDataHandler implements ProtoBufConvertable<ParameterData> {

  private final ParameterData.Builder builder;

  public ParameterDataHandler() {
    builder = ParameterData.newBuilder();
  }

  public void setName(String name) {
    builder.setType(name);
  }

  public void setType(String type) {
    builder.setType(type);
  }

  public void addModifier(String modifier) {
    builder.addModifier(modifier);
  }

  public void addModifiers(List<String> modifiers) {
    builder.addAllModifier(modifiers);
  }

  @Override
  public ParameterData getProtoBufObject() {
    return builder.build();
  }
}
