package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.MethodData;

/**
 * MethodData object holds data from analyzed method.
 */
public class MethodDataHandler implements ProtoBufConvertable<MethodData> {

  // public enum Locality {
  //   CLASS_WIDE,
  //   FILE_WIDE,
  //   PACKAGE_WIDE
  // }

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
    this.builder.addModifiers(modifier);
    // this.modifiers.add(modifier);
  }

  public void addParameter(final String type) {
    this.builder.addParameterList(type);
    // this.parameterList.add(type);
  }

  public void addOutgoingMethodCall(final String fqn) {
    this.builder.addOutgoingMethodCalls(fqn);
    // this.outgoingMethodCalls.add(fqn);
  }

  @Override
  public MethodData getProtoBufObject() {
    return this.builder.build();
  }

  @Override
  public String toString() {
    return "  type: " + this.builder.getReturnType() + "\n"
        + "  modifiers: " + this.builder.getModifiersList() + "\n"
        + "  parameters: " + this.builder.getParameterListList() + "\n"
        + "  outgoing calls: " + this.builder.getOutgoingMethodCallsList();
  }
}
