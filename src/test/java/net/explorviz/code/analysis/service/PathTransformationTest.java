package net.explorviz.code.analysis.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathTransformationTest {

  private final AnalysisService analysisService = new AnalysisService();

  @Test
  void testTransformPathSimple() {
    String original = "app/src/main/java/Utils.java";
    String root = "app/src";
    String expected = "src/main/java/Utils.java";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }

  @Test
  void testTransformPathNoPrefix() {
    String original = "src/main/java/Utils.java";
    String root = "src";
    String expected = "src/main/java/Utils.java";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }

  @Test
  void testTransformPathDeep() {
    String original = "root/sub/app/src/main/java/Utils.java";
    String root = "root/sub/app/src";
    String expected = "src/main/java/Utils.java";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }

  @Test
  void testTransformPathTrailingSlash() {
    String original = "app/src/main/java/Utils.java";
    String root = "app/src/";
    String expected = "src/main/java/Utils.java";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }

  @Test
  void testTransformPathExactMatch() {
    String original = "app/src";
    String root = "app/src";
    String expected = "src";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }

  @Test
  void testTransformPathOutsideRoot() {
    String original = "other/file.java";
    String root = "app/src";
    String expected = "other/file.java";
    Assertions.assertEquals(expected, analysisService.transformPath(original, root));
  }
}
