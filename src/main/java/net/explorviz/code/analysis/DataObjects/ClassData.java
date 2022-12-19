package net.explorviz.code.analysis.DataObjects;

import java.util.ArrayList;
import java.util.List;

public class ClassData {
  String inheritance;
  List<String> interfaces;
  List<String> fields;
  List<ClassData> classDataList;
  List<String> constructorList;
  List<String> methodList;
  List<String> variableList;

  public ClassData() {
    this.inheritance = "";
    this.interfaces = new ArrayList<>();
    this.fields = new ArrayList<>();
    this.classDataList = new ArrayList<>();
    this.constructorList = new ArrayList<>();
    this.methodList = new ArrayList<>();
    this.variableList = new ArrayList<>();
  }

  public void addMethod(String methodName) {
    this.methodList.add(methodName);
  }

  public void setSuperClass(String superClass) {
    this.inheritance = superClass;
  }

  public void addConstructor(String constructor) {
    this.constructorList.add(constructor);
  }

  public void addField(String field) {
    this.fields.add(field);
  }

  @Override
  public String toString() {
    return "{ \n"
        + "methods: " + this.methodList.toString() + "\n"
        + "superClass: " + this.inheritance + "\n"
        + "interfaces: " + this.interfaces.toString() + "\n"
        + "\n}";
  }

  public int getMethodCount(boolean countNested) {
    int methodCount = this.methodList.size();
    if (countNested) {
      for (ClassData classData : this.classDataList) {
        methodCount += classData.getMethodCount(countNested);
      }
    }
    return methodCount;
  }

  public void addImplementedInterface(String implementedInterfaceName) {
    this.interfaces.add(implementedInterfaceName);
  }
}
