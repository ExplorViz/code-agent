package net.explorviz.code.analysis;

import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Collects class names.
 */
@QuarkusTest
public class JavaParserServiceTest {

  @Inject
  JavaParserService parserService;

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeToken; // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecret; // NOCS

}
