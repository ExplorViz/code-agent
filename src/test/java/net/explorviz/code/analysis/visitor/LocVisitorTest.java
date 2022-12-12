package net.explorviz.code.analysis.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Collects class names.
 */
public class LocVisitorTest {

  private LocVisitor visitor;

  @BeforeEach
  void setup() {
    this.visitor = new LocVisitor();
  }

  @Test()
  void testLocSimpleClass() throws IOException {

    final CompilationUnit unit =
        this.createUnitFromContentString(this.loadFromFilePath("files/SimpleTestClass.test"));

    final int expected = 12;

    final int actual = this.visitor.visit(unit, null);

    Assertions.assertEquals(expected, actual);
  }

  @Test()
  void testLocEmptyFile() throws IOException {

    final ClassOrInterfaceDeclaration unit = new ClassOrInterfaceDeclaration();

    final int expected = -1;

    final int actual = this.visitor.visit(unit, null);

    Assertions.assertEquals(expected, actual);
  }

  @Test()
  void testLocWithComments() throws IOException {

    final CompilationUnit unit = this.createUnitFromContentString(
        this.loadFromFilePath("files/SimpleTestClassWithComments.test"));

    final int expected = 12;

    final int actual = this.visitor.visit(unit, null);

    Assertions.assertEquals(expected, actual);
  }


  private CompilationUnit createUnitFromContentString(final String classContent)
      throws IOException {
    final JavaParser javaParser = new JavaParser();

    new CompilationUnit();

    final CompilationUnit unit = javaParser.parse(classContent).getResult().get();
    return unit;
  }

  private String loadFromFilePath(final String filePath) throws IOException {

    ClassLoader loader = ClassLoader.getSystemClassLoader();
    InputStream stream = loader.getResourceAsStream(filePath);
    if (stream == null) {
      System.out.println("Is Null");
    }
    return new String(stream.readAllBytes());
  }

}
