package net.explorviz.code.analysis;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import net.explorviz.code.proto.StructureFileEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Collects class names.
 */
@QuarkusTest
public class JavaParserServiceTest {

  @Inject
  JavaParserService parserService;  // NOCS

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeToken; // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecret; // NOCS

  @Test
  void testProcessStringifiedClass() throws IOException {

    final String expectedClass = new String(ClassLoader.getSystemClassLoader()
        .getResourceAsStream("files/SimpleTestClass.test").readAllBytes());

    final List<StructureFileEvent> actual =
        this.parserService.processStringifiedClass(expectedClass);

    Assertions.assertEquals(1, actual.size());

    final StructureFileEvent actualElement = actual.get(0);

    Assertions.assertEquals(this.landscapeToken, actualElement.getLandscapeToken());
    Assertions.assertEquals(this.landscapeSecret, actualElement.getLandscapeSecret());
    Assertions.assertEquals("my.test.pckg.SimpleTestClass",
        actualElement.getFullyQualifiedOperationName());

    Assertions.assertEquals(expectedClass,
        actualElement.getArgumentsOrDefault("content-file", "no-class-content"));
    Assertions.assertEquals("12",
        actualElement.getArgumentsOrDefault("count-code-lines", "no-count-code-lines"));
    Assertions.assertEquals("1",
        actualElement.getArgumentsOrDefault("count-methods", "no-count-methods"));
    Assertions.assertEquals("1",
        actualElement.getArgumentsOrDefault("count-fields", "no-count-fields"));
  }

}
