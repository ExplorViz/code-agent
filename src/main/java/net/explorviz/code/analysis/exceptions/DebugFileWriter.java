package net.explorviz.code.analysis.exceptions;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.YamlPrinter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.explorviz.code.analysis.parser.JavaParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Class to write failing file content to files.
 */
public final class DebugFileWriter {
  public static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);

  private DebugFileWriter() {

  }

  /**
   * Saves the given content to a file.
   *
   * @param directoryPath the path where the file should be created
   * @param content the content that should be written
   * @param filename the name of the file
   */
  public static void saveDebugFile(final String directoryPath, final String content,
                                   final String filename) {
    try {
      Files.write(Paths.get(directoryPath, filename), content.getBytes());
    } catch (IOException e) {
      if (LOGGER.isErrorEnabled()) {
        // if something happens here, catch and ignore as the program can continue as normal
        // only the debug file will not be present
        LOGGER.error("Unable to write the content to file.");
      }
    }
  }

  public static void saveAstAsYaml(final CompilationUnit compilationUnit, final String path) {
    YamlPrinter printer = new YamlPrinter(true);
    try {
      Files.write(Paths.get(path), printer.output(compilationUnit).getBytes());
    } catch (IOException e) {
      if (LOGGER.isErrorEnabled()) {
        // if something happens here, catch and ignore as the program can continue as normal
        // only the debug file will not be present
        LOGGER.error("Unable to write the content to file.");
      }
    }
  }
}
