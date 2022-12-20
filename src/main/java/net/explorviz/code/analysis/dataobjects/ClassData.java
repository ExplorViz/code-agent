package net.explorviz.code.analysis.dataobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassData object holds data from analyzed classes.
 */
public class ClassData {

  /**
   * Enum to differentiate the types of java "classes".
   */
  public enum ClassType {
    INTERFACE, ABSTRACT_CLASS, CLASS
  }

  private ClassType type;
  private final List<String> modifiers;
  private String superClass;
  private final List<String> interfaces;
  private final List<String> fields;
  private final List<String> innerClasses;
  private final List<String> constructorList;
  private final List<String> methodList;
  private final Map<String, MethodData> methodDataMap;
  private final List<String> variableList;

  private int loc = 0;

  /**
   * Creates a blank ClassData object.
   */
  public ClassData() {
    this.superClass = "";
    this.modifiers = new ArrayList<>();
    this.interfaces = new ArrayList<>();
    this.fields = new ArrayList<>();
    this.innerClasses = new ArrayList<>();
    this.constructorList = new ArrayList<>();
    this.methodList = new ArrayList<>();
    this.methodDataMap = new HashMap<>();
    this.variableList = new ArrayList<>();
  }

  public void addMethod(final String methodName) {
    this.methodList.add(methodName);
  }

  public void setSuperClass(final String superClass) {
    this.superClass = superClass;
  }

  public void addConstructor(final String constructor) {
    this.constructorList.add(constructor);
  }

  public void addField(final String field) {
    this.fields.add(field);
  }

  public void addModifier(final String modifier) {
    this.modifiers.add(modifier);
  }

  public void addInnerClass(final String name) {
    this.innerClasses.add(name);
  }

  public int getMethodCount() {
    return this.methodList.size();
  }

  public void addImplementedInterface(final String implementedInterfaceName) {
    this.interfaces.add(implementedInterfaceName);
  }

  public void setIsInterface() {
    this.type = ClassType.INTERFACE;
  }

  public void setIsAbstractClass() {
    this.type = ClassType.ABSTRACT_CLASS;
  }

  public void setIsClass() {
    this.type = ClassType.CLASS;
  }

  public void setLoc(int loc) {
    this.loc = loc;
  }

  @Override
  public String toString() {
    return "{ \n"
        + "type: " + type.toString() + "\n"
        + "modifier: " + modifiers.toString() + "\n"
        + "superClass: " + this.superClass + "\n"
        + "interfaces: " + this.interfaces.toString() + "\n"
        + "fields: " + this.fields.toString() + "\n"
        + "innerClasses: " + this.innerClasses.toString() + "\n"
        + "constructor: " + this.constructorList.toString() + "\n"
        + "methods: " + this.methodList.toString() + "\n"
        + "variables: " + this.variableList.toString() + "\n"
        + "loc: " + this.loc + "\n}";
  }
}
