package net.explorviz.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.visitor.MultiCollectorVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser Object loads and parses .java files.
 */
@ApplicationScoped
public class JavaParserService {


  public static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);
  private static final String FILE_PATH =
      "C:\\Users\\Julian\\projects\\Bachelor\\Fooling\\src\\main\\java";

  private FileDataHandler parse(final CompilationUnit compilationUnit, final String fileName) {
    final FileDataHandler data = new FileDataHandler(fileName);
    final MultiCollectorVisitor multiCollectorVisitor = new MultiCollectorVisitor();
    multiCollectorVisitor.visit(compilationUnit, data);
    // TODO: for testing only!
    return data;
  }

  /**
   * Parses a given file completely. HARDCODED TEST!! DO NOT USE NOW!
   *
   * @param fileContent stringified java file
   */
  public FileDataHandler fullParse(final String fileContent, final String fileName) {
    // final TypeSolver typeSolver = new CombinedTypeSolver(
    //     new ReflectionTypeSolver(),
    //     new JavaParserTypeSolver(
    //         new File(FILE_PATH))
    // );
    final TypeSolver typeSolver = new CombinedTypeSolver(
        new ReflectionTypeSolver()
    );
    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    final CompilationUnit compilationUnit = StaticJavaParser.parse(fileContent);
    // LOGGER.info();
    FileDataHandler fileData = null;
    // fileData = parse(compilationUnit);
    try {
      fileData = parse(compilationUnit, fileName);
    } catch (NoSuchElementException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("NoSuchElementException: \n" + compilationUnit.toString());
      }
    } catch (NoSuchFieldError e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("NoSuchFieldError: \n" + compilationUnit.toString());
      }
    } catch (Exception | Error e) { // NOPMD
      // LOGGER.error("ERROR!");
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(e.getClass().toString());
      }
      // LOGGER.error( e.getClass().toString() + ": \n" + compilationUnit.toString());
    }
    return fileData;

  }

  /**
   * Parses a test file completely. HARDCODED TEST!! DO NOT USE NOW!
   *
   * @throws IOException Gets thrown if the file is not reachable
   */
  public FileDataHandler fullParse() throws IOException {
    final Path path = Path.of(
        "C:\\Users\\Julian\\projects\\Bachelor\\Fooling\\src\\main\\java\\org\\example\\Main.java");
    // final String classContent = Files.readString(path);
    final TypeSolver typeSolver = new CombinedTypeSolver(
        new ReflectionTypeSolver(),
        new JavaParserTypeSolver(
            new File(FILE_PATH))
    );
    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    // StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    final CompilationUnit compilationUnit = StaticJavaParser.parse(path);
    return parse(compilationUnit, "");
  }

}
