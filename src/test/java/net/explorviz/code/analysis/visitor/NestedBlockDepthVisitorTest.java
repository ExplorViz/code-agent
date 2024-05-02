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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the NestedBlockDepthVisitor.
 */
@QuarkusTest
public class NestedBlockDepthVisitorTest {
  @Test()
  void nestedBlockDepth() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("Nested.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty(), false);
    String path = "src/test/resources/files/Nested.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    NestedBlockDepthVisitor nestedBlockDepth = new NestedBlockDepthVisitor();
    MetricAppender appender = new MetricAppender(fileDataHandler);
    nestedBlockDepth.visit(compilationUnit, new Pair<>(appender, null));
    Assertions.assertEquals("6", fileDataHandler.getClassData("com.easy.life.Nested") // NOCS
        .getMethod("com.easy.life.Nested.heavyNested#1")
        .getMetricValue("nestedBlockDepth")); // NOCS
    Assertions.assertEquals("4", fileDataHandler.getClassData("com.easy.life.Nested") // NOCS
        .getMethod("com.easy.life.Nested.heavyNested2#1980e")
        .getMetricValue("nestedBlockDepth")); // NOCS

  }
}
