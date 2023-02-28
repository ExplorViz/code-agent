package net.explorviz.code.analysis.git;

import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Testing the repository loader.
 */
@QuarkusTest
public class DirectoryFinderTest {

  private static final String MAIN_SOURCE_PATH = "/src/main/java";
  private static final String JAVA = "java";
  private static final String SRC = "src";
  private static final String REGEX_CONTAINS_SLASH = "\\\\+|/+";
  private static final String NOT_FOUND = "Not Found";

  private static File tempLocation;
  private static DirectoryFinder directoryFinder;

  @BeforeAll
  static void setup() throws IOException {
    tempLocation = Files.createTempDirectory("explorviz-test").toFile();
    Files.createDirectories(Paths.get(tempLocation.getAbsolutePath(), SRC, "main", JAVA));
    Files.createDirectories(Paths.get(tempLocation.getAbsolutePath(), SRC, "test", JAVA));
  }


  @AfterAll
  static void tearDown() throws IOException {
    try (Stream<Path> walk = Files.walk(tempLocation.toPath())) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile)
          .forEach(File::delete);
    }
    DirectoryFinder.reset();
  }


  @Test()
  void testRelativePath() {
    List<String> searchPaths = new ArrayList<>();
    searchPaths.add(MAIN_SOURCE_PATH);
    try {
      List<String> absolutePaths = DirectoryFinder.getDirectory(searchPaths,
          tempLocation.getAbsolutePath());
      Assertions.assertEquals(1, absolutePaths.size());
      // clean string from mutiple slashes and bring to system seperator
      String s = (tempLocation.getAbsolutePath() + searchPaths.get(0)).replaceAll(
          REGEX_CONTAINS_SLASH,
          Matcher.quoteReplacement(File.separator));
      Assertions.assertEquals(absolutePaths.get(0), s);
    } catch (NotFoundException e) {
      Assertions.fail(NOT_FOUND);
    }
  }

  @Test()
  void testLeadingWildcardPath() {
    List<String> searchPaths = new ArrayList<>();
    searchPaths.add("*/main/java");
    try {
      List<String> absolutePaths = DirectoryFinder.getDirectory(searchPaths,
          tempLocation.getAbsolutePath());
      Assertions.assertEquals(1, absolutePaths.size());
      String s = (tempLocation.getAbsolutePath() + MAIN_SOURCE_PATH).replaceAll(
          REGEX_CONTAINS_SLASH,
          Matcher.quoteReplacement(File.separator));
      Assertions.assertEquals(absolutePaths.get(0), s);
    } catch (NotFoundException e) {
      Assertions.fail(NOT_FOUND);
    }
  }

  @Test()
  void testInfixWildcardPath() {
    List<String> searchPaths = new ArrayList<>();
    searchPaths.add("src/*/java");
    try {
      List<String> absolutePaths = DirectoryFinder.getDirectory(searchPaths,
          tempLocation.getAbsolutePath());
      Assertions.assertEquals(1, absolutePaths.size());
      String s = (tempLocation.getAbsolutePath() + MAIN_SOURCE_PATH).replaceAll(
          REGEX_CONTAINS_SLASH,
          Matcher.quoteReplacement(File.separator));
      Assertions.assertEquals(absolutePaths.get(0), s);
    } catch (NotFoundException e) {
      Assertions.fail(NOT_FOUND);
    }
  }

  @Test()
  void testConsecutiveInfixWildcardPath() {
    List<String> searchPaths = new ArrayList<>();
    searchPaths.add("src/*/*/java");
    try {
      List<String> absolutePaths = DirectoryFinder.getDirectory(searchPaths,
          tempLocation.getAbsolutePath());
      Assertions.assertEquals(1, absolutePaths.size());
      String s = (tempLocation.getAbsolutePath() + MAIN_SOURCE_PATH).replaceAll(
          REGEX_CONTAINS_SLASH,
          Matcher.quoteReplacement(File.separator));
      Assertions.assertEquals(absolutePaths.get(0), s);
    } catch (NotFoundException e) {
      Assertions.fail(NOT_FOUND);
    }
  }

  @Test()
  void testConsecutiveInfixWildcardUnresolvablePath() {
    List<String> searchPaths = new ArrayList<>();
    searchPaths.add("src/*/*/main/java");
    Assertions.assertThrows(NotFoundException.class, () -> DirectoryFinder.getDirectory(searchPaths,
        tempLocation.getAbsolutePath()));
  }
}
