package net.explorviz.code.analysis.dataobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * MethodData object holds data from analyzed method.
 */
public class MethodData {

  private final String returnType;
  private final List<String> modifiers;
  private final List<String> parameterList;
  private final List<String> outgoingMethodCalls;

  /**
   * Creates a new MethodData object holding data describing the method.
   *
   * @param returnType the return type of the method
   */
  public MethodData(final String returnType) {
    this.returnType = returnType;
    this.modifiers = new ArrayList<>();
    this.parameterList = new ArrayList<>();
    this.outgoingMethodCalls = new ArrayList<>();
  }

  public void addModifier(final String modifier) {
    this.modifiers.add(modifier);
  }

  public void addParameter(final String type) {
    this.parameterList.add(type);
  }

  public void addOutgoingMethodCall(final String fqn) {
    this.outgoingMethodCalls.add(fqn);
  }

  @Override
  public String toString() {
    return "  type: " + this.returnType + "\n"
        + "  modifiers: " + this.modifiers + "\n"
        + "  parameters: " + this.parameterList + "\n"
        + "  outgoing calls: " + this.outgoingMethodCalls + "\n";
  }
}
