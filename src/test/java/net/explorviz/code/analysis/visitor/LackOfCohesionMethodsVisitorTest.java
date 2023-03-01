package net.explorviz.code.analysis.visitor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Optional;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MetricAppender;
import net.explorviz.code.proto.ClassData;
import net.explorviz.code.proto.FileData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LackOfCohesionMethodsVisitor calculating the LCOM4 metric.
 */
@QuarkusTest
public class LackOfCohesionMethodsVisitorTest {

  private static final String LCOM4 = "LCOM4";

  @Test()
  void fileDataTest1() throws FileNotFoundException { // NOCS
    FileDataHandler fileDataHandler = new FileDataHandler("LCOM4.java");
    FileDataVisitor visitor = new FileDataVisitor(Optional.empty());
    String path = "src/test/resources/files/LCOM4.java";
    final CompilationUnit compilationUnit = StaticJavaParser.parse(new File(path));
    visitor.visit(compilationUnit, fileDataHandler);
    LackOfCohesionMethodsVisitor lcom4Visitor = new LackOfCohesionMethodsVisitor();
    lcom4Visitor.visit(compilationUnit, new Pair<>(new MetricAppender(fileDataHandler), null));
    FileData data = fileDataHandler.getProtoBufObject();
    Map<String, ClassData> classDataMap = data.getClassDataMap();
    Assertions.assertEquals("1",
        classDataMap.get("com.easy.life.LCOM4").getMetricMap().get(LCOM4));
    Assertions.assertEquals("1",
        classDataMap.get("com.easy.life.LCOM4Class2").getMetricMap().get(LCOM4));
    Assertions.assertEquals("1",
        classDataMap.get("com.easy.life.LCOM4Class3").getMetricMap().get(LCOM4));
    Assertions.assertEquals("0",
        classDataMap.get("com.easy.life.LCOM4Class4").getMetricMap().get(LCOM4));
    Assertions.assertEquals("1",
        classDataMap.get("com.easy.life.LCOM4Class5").getMetricMap().get(LCOM4));
    Assertions.assertEquals("6",
        classDataMap.get("com.easy.life.LCOM4Class6").getMetricMap().get(LCOM4));
  }
}
