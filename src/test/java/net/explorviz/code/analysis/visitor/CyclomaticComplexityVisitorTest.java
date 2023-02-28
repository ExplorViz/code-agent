package net.explorviz.code.analysis.visitor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MetricAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing the CyclomaticComplexityVisitor.
 */
@QuarkusTest
public class CyclomaticComplexityVisitorTest { // NOCS
  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test()
  void cyclomaticComplexityTest() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("Nested.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty());
    String path = "src/test/resources/files/Nested.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    CyclomaticComplexityVisitor cyclomaticComplexityVisitor = new CyclomaticComplexityVisitor();
    MetricAppender appender = new MetricAppender(fileDataHandler);
    cyclomaticComplexityVisitor.visit(compilationUnit, new Pair<>(appender, null));
    Assertions.assertEquals("6", fileDataHandler.getClassData("com.easy.life.Nested") // NOCS
        .getMethod("com.easy.life.Nested.heavyNested#1")
        .getMetricValue("cyclomatic_complexity")); // NOCS
    Assertions.assertEquals("2", fileDataHandler.getClassData("com.easy.life.Nested") // NOCS
        .getMethod("com.easy.life.Nested.heavyNested2#80")
        .getMetricValue("cyclomatic_complexity"));// NOCS
    Assertions.assertEquals("4", fileDataHandler.getClassData("com.easy.life.Nested") // NOCS
        .getMetricValue("cyclomatic_complexity_weighted"));

  }
}
