package net.explorviz.code.analysis.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Collects class names.
 */
public class ClassNameVisitorTest {

  private ClassNameVisitor visitor;

  @BeforeEach
  void setup() {

    this.visitor = new ClassNameVisitor();

  }

  private CompilationUnit createUnit() {
    final JavaParser javaParser = new JavaParser();

    final CompilationUnit unit =
        javaParser.parse("public class Test\n" + "{\n" + "   public class InnerTest\n   {\n"
            + "       public InnerTest() {}\n" + "   }\n" + "    \n" + "   public Test() {\n"
            + "   }\n\n" + "   public static void main( String[] args ) { \n"
            + "       new Test().new InnerTest();\n   }\n" + "}").getResult().get(); //NOCS
    return unit;
  }

  @Test()
  void testFindAllSize() throws IOException {
    final CompilationUnit unit = this.createUnit();

    final List<String> expected = Arrays.asList("Test.InnerTest", "Test");

    final List<String> actual = new ArrayList<>();
    this.visitor.visit(unit, actual);

    Assertions.assertEquals(expected, actual);
  }

}
