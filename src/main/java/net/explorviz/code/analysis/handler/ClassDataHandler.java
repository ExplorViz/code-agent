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

  // private ClassType type;
  // private String superClass;
  // private final List<String> modifiers;
  // private final List<String> interfaces;
  // private final List<String> fields;
  // private final List<String> innerClasses;
  // private final List<String> constructorList;
  // private final List<String> variableList;
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
   * @param methodFqn the method's fully qualified name
   * @param returnType the return type of the method
   * @return the created methodData object
   */
  public MethodDataHandler addMethod(final String methodFqn, final String returnType) {
    this.methodDataMap.put(methodFqn, new MethodDataHandler(returnType));
    return methodDataMap.get(methodFqn);
  }

  public void setSuperClass(final String superClass) {
    this.builder.setSuperClass(superClass);
    // this.superClass = superClass;
  }

  public void addConstructor(final String constructor) {
    this.builder.addConstructorList(constructor);
    // this.constructorList.add(constructor);
  }

  public void addField(final String fieldName, final String fieldType,
                       final List<String> modifiers) {
    this.builder.addFields(
        FieldData.newBuilder().setName(fieldName).setType(fieldType).addAllModifiers(modifiers));
  }

  public void addModifier(final String modifier) {
    this.builder.addModifiers(modifier);
    // this.modifiers.add(modifier);
  }

  public void addInnerClass(final String name) {
    this.builder.addInnerClasses(name);
    // this.innerClasses.add(name);
  }

  public void addEnumConstant(final String name) {
    this.builder.addEnumConstants(name);
  }

  public int getMethodCount() {
    return this.methodDataMap.size();
  }

  public void addImplementedInterface(final String implementedInterfaceName) {
    this.builder.addInterfaces(implementedInterfaceName);
    // this.interfaces.add(implementedInterfaceName);
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

  public void setIsClass() {
    this.builder.setType(ClassType.CLASS);
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


  public void setLoc(final int loc) {
    this.builder.setLoc(loc);
    // this.loc = loc;
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
    return "{ \n"
        + "type: " + this.builder.getType().toString() + "\n"
        + "modifier: " + this.builder.getModifiersList() + "\n"
        + "superClass: " + this.builder.getSuperClass() + "\n"
        + "interfaces: " + this.builder.getInterfacesList() + "\n"
        + "fields: " + this.builder.getFieldsList() + "\n"
        + "innerClasses: " + this.builder.getInnerClassesList() + "\n"
        + "constructors: " + this.builder.getConstructorListList() + "\n"
        + "methods: \n" + methodDataString + "\n"
        + "variables: " + this.builder.getVariableListList() + "\n"
        + "loc: " + this.builder.getLoc() + "\n}";
  }


}
