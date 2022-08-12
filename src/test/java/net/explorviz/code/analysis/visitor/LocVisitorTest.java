package net.explorviz.code.analysis.visitor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
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

  private CompilationUnit createUnit() throws IOException {
    final JavaParser javaParser = new JavaParser();

    final String expectedClass = new String(ClassLoader.getSystemClassLoader()
        .getResourceAsStream("files/SimpleTestClass.test").readAllBytes());

    final CompilationUnit unit = javaParser.parse(expectedClass).getResult().get();
    return unit;
  }

  @Test()
  void testFindAllSize() throws IOException {
    final CompilationUnit unit = this.createUnit();

    final int expected = 12;

    final int actual = this.visitor.visit(unit, null);

    Assertions.assertEquals(expected, actual);
  }

}
