package net.explorviz.code.analysis.visitor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.proto.ClassData;
import net.explorviz.code.proto.ClassType;
import net.explorviz.code.proto.FieldData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.MethodData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the FileDataVisitor.
 */
@QuarkusTest
public class FileDataVisitorTest {

  @Test()
  void fileDataTest1() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("Nested.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty(), false);
    String path = "src/test/resources/files/Nested.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    FileData data = fileDataHandler.getProtoBufObject();
    Assertions.assertEquals("com.easy.life", data.getPackageName());  // NOCS
    Assertions.assertEquals(0, data.getImportNameCount());
    Assertions.assertEquals(1, data.getClassDataCount());
    Map<String, ClassData> classDataMap = data.getClassDataMap();
    ClassData clazz = classDataMap.get("com.easy.life.Nested");
    Assertions.assertTrue(clazz.containsMethodData("com.easy.life.Nested.heavyNested#1"));
    Assertions.assertTrue(
        clazz.containsMethodData("com.easy.life.Nested.heavyNested2#1980e")); // NOCS
    Assertions.assertEquals("int",
        clazz.getMethodDataMap().get("com.easy.life.Nested.heavyNested2#1980e").getParameterList()
            .get(0).getType());
  }

  @Test()
  void fileDataTest2() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("Happy.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty(), false);
    String path = "src/test/resources/files/Happy.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    FileData data = fileDataHandler.getProtoBufObject();
    Assertions.assertEquals("com.easy.life", data.getPackageName());
    Assertions.assertEquals(4, data.getImportNameCount());  // NOCS
    Assertions.assertEquals(1, data.getClassDataCount());
    Map<String, ClassData> classDataMap = data.getClassDataMap();
    ClassData clazz = classDataMap.get("com.easy.life.Happy");
    Assertions.assertTrue(
        clazz.containsMethodData("com.easy.life.Happy.fromArrayToList#146d5")); // NOCS
    Assertions.assertEquals("java.util.ArrayList<T>",
        clazz.getMethodDataMap().get("com.easy.life.Happy.fromArrayToList#146d5").getReturnType());
  }

  @Test()
  void fileDataEnumTest() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("ColorParam.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty(), false);
    String path = "src/test/resources/files/ColorParam.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    FileData data = fileDataHandler.getProtoBufObject();
    Assertions.assertEquals("net.sourceforge.plantuml", data.getPackageName());
    Map<String, ClassData> classDataMap = data.getClassDataMap();
    ClassData clazz = classDataMap.get("net.sourceforge.plantuml.ColorParam");
    Assertions.assertSame(clazz.getType(), ClassType.ENUM);
    Assertions.assertTrue(contains(clazz.getFieldList(), "isBackground"));
    Assertions.assertTrue(containsConstant(clazz.getEnumConstantList(), "classArrow"));
    Assertions.assertTrue(containsConstant(clazz.getEnumConstantList(), "noteBorder"));
  }

  @Test()
  void fileDataAnnotationTest() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("SimpleJdbcClinic.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty(), false);
    String path = "src/test/resources/files/SimpleJdbcClinic.java";
    StaticJavaParser.getParserConfiguration()
        .setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    FileData data = fileDataHandler.getProtoBufObject();
    // Assertions.assertEquals("net.sourceforge.plantuml", data.getPackageName());
    Map<String, ClassData> classDataMap = data.getClassDataMap();
    ClassData clazz = classDataMap.get(
        "org.springframework.samples.petclinic.jdbc.SimpleJdbcClinic");
    // Assertions.assertTrue(contains(clazz.getFieldList(), "isBackground"));
    Assertions.assertTrue(containsConstant(clazz.getAnnotationList(), "Service"));
    // TODO how to handle annotations with content
    // Assertions.assertTrue(
    // containsConstant(clazz.getAnnotationList(), "@ManagedResource(\"petclinic:type=Clinic\")"));
    MethodData method = clazz.getMethodDataOrThrow(
        "org.springframework.samples.petclinic.jdbc.SimpleJdbcClinic.init#c2aa38a4");

    Assertions.assertTrue(containsConstant(method.getAnnotationList(), "Autowired"));
  }

  private boolean contains(final List<FieldData> fields, final String value) {
    for (final FieldData field : fields) {
      if (field.getName().equals(value)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsConstant(final List<String> list, final String value) {
    for (final String entry : list) {
      if (entry.equals(value)) {
        return true;
      }
    }
    return false;
  }
}
