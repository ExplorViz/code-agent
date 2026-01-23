package net.explorviz.code.analysis.visitor;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.explorviz.code.analysis.handler.JavaFileDataHandler;
import net.explorviz.code.analysis.parser.AntlrParserService;
import net.explorviz.code.proto.ClassData;
import net.explorviz.code.proto.ClassType;
import net.explorviz.code.proto.FieldData;
import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.FunctionData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ANTLR-based FileDataListener.
 * These tests verify that the ANTLR implementation extracts the same data
 * as the JavaParser-based FileDataVisitor.
 */
@QuarkusTest
public class JavaFileDataListenerTest {

        @Inject
        AntlrParserService antlrParserService;

        @Test
        void fileDataTest1_Nested() throws IOException {
                final String path = "src/test/resources/files/Nested.java";
                final String fileContent = Files.readString(Path.of(path));

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                fileContent, "Nested.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler, "JavaFileDataHandler should not be null");

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify package
                Assertions.assertEquals("com.easy.life", data.getPackageName());

                // Verify imports (this file has no imports)
                Assertions.assertEquals(0, data.getImportNamesCount());

                // Verify class count
                Assertions.assertEquals(1, data.getClassesCount());

                // Verify class data
                final ClassData clazz = findClass(data.getClassesList(), "com.easy.life.Nested");
                Assertions.assertNotNull(clazz, "Class com.easy.life.Nested should exist");

                // Verify methods exist
                Assertions.assertTrue(containsFunction(clazz.getFunctionsList(), "com.easy.life.Nested.heavyNested#1"),
                                "Method heavyNested#1 should exist");
                Assertions.assertTrue(
                                containsFunction(clazz.getFunctionsList(), "com.easy.life.Nested.heavyNested2#1980e"),
                                "Method heavyNested2#1980e should exist");

                // Verify parameter type
                final FunctionData method = findFunction(clazz.getFunctionsList(),
                                "com.easy.life.Nested.heavyNested2#1980e");
                Assertions.assertNotNull(method, "Method heavyNested2 should exist");
                Assertions.assertEquals(1, method.getParametersCount(), "Method should have 1 parameter");
                Assertions.assertTrue(method.getParameters(0).getType().contains("int"),
                                "Parameter type should be int (may have package prefix)");
        }

        @Test
        void fileDataTest2_Happy() throws IOException {
                final String path = "src/test/resources/files/Happy.java";
                final String fileContent = Files.readString(Path.of(path));

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                fileContent, "Happy.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler, "JavaFileDataHandler should not be null");

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify package
                Assertions.assertEquals("com.easy.life", data.getPackageName());

                // Verify imports
                Assertions.assertEquals(4, data.getImportNamesCount(), "Should have 4 imports");

                // Verify class count
                Assertions.assertEquals(1, data.getClassesCount());

                // Verify class data
                final ClassData clazz = findClass(data.getClassesList(), "com.easy.life.Happy");
                Assertions.assertNotNull(clazz, "Class com.easy.life.Happy should exist");

                // Verify method exists
                Assertions.assertTrue(
                                containsFunction(clazz.getFunctionsList(), "com.easy.life.Happy.fromArrayToList#146d5"),
                                "Method fromArrayToList should exist");

                // Verify return type
                final FunctionData method = findFunction(clazz.getFunctionsList(),
                                "com.easy.life.Happy.fromArrayToList#146d5");
                Assertions.assertNotNull(method, "Method fromArrayToList should exist");

                // Note: The actual declared return type in the source is List<T>, not
                // ArrayList<T>
                // (ArrayList is only used in the implementation: return new ArrayList<>())
                // ANTLR correctly extracts the declared return type: java.util.List<T>
                final String returnType = method.getReturnType();
                Assertions.assertTrue(returnType.contains("List"),
                                "Return type should contain List, got: " + returnType);
                Assertions.assertTrue(returnType.contains("<T>"),
                                "Return type should preserve generic parameter <T>");
        }

        @Test
        void fileDataEnumTest_ColorParam() throws IOException {
                final String path = "src/test/resources/files/ColorParam.java";
                final String fileContent = Files.readString(Path.of(path));

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                fileContent, "ColorParam.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler, "JavaFileDataHandler should not be null");

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify package
                Assertions.assertEquals("net.sourceforge.plantuml", data.getPackageName());

                // Verify class data
                final ClassData clazz = findClass(data.getClassesList(), "net.sourceforge.plantuml.ColorParam");
                Assertions.assertNotNull(clazz, "Enum ColorParam should exist");

                // Verify it's an enum
                Assertions.assertEquals(ClassType.ENUM, clazz.getType(),
                                "ColorParam should be an ENUM");

                // Verify fields
                Assertions.assertTrue(containsField(clazz.getFieldsList(), "isBackground"),
                                "Field isBackground should exist");

                // Verify enum constants
                Assertions.assertTrue(containsConstant(clazz.getEnumValuesList(), "classArrow"),
                                "Enum constant classArrow should exist");
                Assertions.assertTrue(containsConstant(clazz.getEnumValuesList(), "noteBorder"),
                                "Enum constant noteBorder should exist");
        }

        @Test
        void fileDataAnnotationTest_SimpleJdbcClinic() throws IOException {
                final String path = "src/test/resources/files/SimpleJdbcClinic.java";
                final String fileContent = Files.readString(Path.of(path));

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                fileContent, "SimpleJdbcClinic.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler, "JavaFileDataHandler should not be null");

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify class data
                final ClassData clazz = findClass(data.getClassesList(),
                                "org.springframework.samples.petclinic.jdbc.SimpleJdbcClinic");
                Assertions.assertNotNull(clazz, "SimpleJdbcClinic class should exist");

                // Check that a specific method exists
                final boolean methodExists = clazz.getFunctionsList().stream()
                                .anyMatch(f -> f.getName().contains("init"));
                Assertions.assertTrue(methodExists, "Method init should exist");
        }

        @Test
        void testBasicStructure_ClassWithMethodsAndFields() throws IOException {
                // Create a simple test Java file content
                final String javaCode = """
                                package com.test;

                                import java.util.List;
                                import java.util.ArrayList;

                                public class TestClass {
                                    private String name;
                                    private int count;

                                    public TestClass(String name) {
                                        this.name = name;
                                    }

                                    public String getName() {
                                        return name;
                                    }

                                    public void setName(String name) {
                                        this.name = name;
                                    }

                                    public int getCount() {
                                        return count;
                                    }
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "TestClass.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify package
                Assertions.assertEquals("com.test", data.getPackageName());

                // Verify imports
                Assertions.assertEquals(2, data.getImportNamesCount());
                Assertions.assertTrue(data.getImportNamesList().contains("java.util.List"));
                Assertions.assertTrue(data.getImportNamesList().contains("java.util.ArrayList"));

                // Verify class
                final ClassData clazz = findClass(data.getClassesList(), "com.test.TestClass");
                Assertions.assertNotNull(clazz);
                Assertions.assertEquals(ClassType.CLASS, clazz.getType());

                // Verify fields
                Assertions.assertEquals(2, clazz.getFieldsCount());
                Assertions.assertTrue(containsField(clazz.getFieldsList(), "name"));
                Assertions.assertTrue(containsField(clazz.getFieldsList(), "count"));

                // Verify methods (3 methods + 1 constructor)
                Assertions.assertTrue(clazz.getFunctionsCount() >= 3,
                                "Should have at least 3 methods");

                // Verify constructor exists
                final boolean hasConstructor = clazz.getFunctionsList().stream()
                                .anyMatch(f -> f.getName().contains("TestClass"));
                Assertions.assertTrue(hasConstructor, "Constructor should exist");
        }

        @Test
        void testInterface() throws IOException {
                final String javaCode = """
                                package com.test;

                                public interface TestInterface {
                                    void doSomething();
                                    String getValue();
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "TestInterface.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();
                final ClassData interfaceData = findClass(data.getClassesList(), "com.test.TestInterface");

                Assertions.assertNotNull(interfaceData);
                Assertions.assertEquals(ClassType.INTERFACE, interfaceData.getType());

                // Verify methods
                Assertions.assertTrue(interfaceData.getFunctionsCount() >= 2);
        }

        @Test
        void testEnum() throws IOException {
                final String javaCode = """
                                package com.test;

                                public enum Color {
                                    RED, GREEN, BLUE;

                                    private String hexValue;

                                    public String getHexValue() {
                                        return hexValue;
                                    }
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "Color.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();
                final ClassData enumData = findClass(data.getClassesList(), "com.test.Color");

                Assertions.assertNotNull(enumData);
                Assertions.assertEquals(ClassType.ENUM, enumData.getType());

                // Verify enum constants
                Assertions.assertEquals(3, enumData.getEnumValuesCount());
                Assertions.assertTrue(containsConstant(enumData.getEnumValuesList(), "RED"));
                Assertions.assertTrue(containsConstant(enumData.getEnumValuesList(), "GREEN"));
                Assertions.assertTrue(containsConstant(enumData.getEnumValuesList(), "BLUE"));
        }

        @Test
        void testInheritance() throws IOException {
                final String javaCode = """
                                package com.test;

                                public class Child extends Parent implements Interface1, Interface2 {
                                    public void childMethod() {
                                    }
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "Child.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();
                final ClassData childClass = findClass(data.getClassesList(), "com.test.Child");

                Assertions.assertNotNull(childClass);

                // Verify superclass
                Assertions.assertTrue(childClass.getSuperclassesList().stream().anyMatch(s -> s.contains("Parent")),
                                "Should have Parent as superclass");

                // Verify interfaces
                Assertions.assertEquals(2, childClass.getImplementedInterfacesCount(),
                                "Should implement 2 interfaces");
        }

        @Test
        void testLOCMetric() throws IOException {
                final String javaCode = """
                                package com.test;

                                public class SimpleClass {
                                    public void method1() {
                                        System.out.println("Line 1");
                                        System.out.println("Line 2");
                                    }
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "SimpleClass.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify LOC metric exists
                Assertions.assertTrue(data.containsMetrics("loc"), "LOC metric should exist");
                final Double locValue = data.getMetricsMap().get("loc");
                Assertions.assertNotNull(locValue);

                // LOC should be greater than 0
                Assertions.assertTrue(locValue > 0.0, "LOC should be greater than 0");
        }

        @Test
        void testNestedClass() throws IOException {
                final String javaCode = """
                                package com.test;

                                public class Outer {
                                    private class Inner {
                                        void innerMethod() {
                                        }
                                    }

                                    public void outerMethod() {
                                    }
                                }
                                """;

                final JavaFileDataHandler fileDataHandler = antlrParserService.parseFileContent(
                                javaCode, "Outer.java", "test-commit");

                Assertions.assertNotNull(fileDataHandler);

                final FileData data = fileDataHandler.getProtoBufObject();

                // Verify both classes exist
                Assertions.assertTrue(data.getClassesCount() >= 1,
                                "Should have at least the outer class");

                // Outer class should exist
                final ClassData outerClass = findClass(data.getClassesList(), "com.test.Outer");
                Assertions.assertNotNull(outerClass, "Outer class should exist");

                // Inner class should be in the outer class's inner_classes list
                final boolean hasInnerClass = outerClass.getInnerClassesList().stream()
                                .anyMatch(c -> c.getName().equals("com.test.Outer.Inner"));
                Assertions.assertTrue(hasInnerClass, "Inner class should be tracked as a nested class");
        }

        // Helper methods

        private ClassData findClass(List<ClassData> classes, String name) {
                return classes.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        }

        private FunctionData findFunction(List<FunctionData> functions, String name) {
                return functions.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
        }

        private boolean containsFunction(List<FunctionData> functions, String name) {
                return functions.stream().anyMatch(f -> f.getName().equals(name));
        }

        private boolean containsField(final List<FieldData> fields, final String fieldName) {
                return fields.stream().anyMatch(field -> field.getName().equals(fieldName));
        }

        private boolean containsConstant(final List<String> list, final String value) {
                return list.contains(value);
        }
}
