package net.explorviz.code.analysis.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
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

  private String sourcePath;
  private JavaSymbolSolver javaSymbolSolver;
  private CombinedTypeSolver combinedTypeSolver;
  private ReflectionTypeSolver reflectionTypeSolver;
  private JavaParserTypeSolver javaParserTypeSolver;

  /**
   * Creates a new JavaParserService without any TypeSolvers, call {@link JavaParserService#reset()}
   * or {@link JavaParserService#reset(String)} to get a valid JavaParserService.
   */
  public JavaParserService() {
    combinedTypeSolver = new CombinedTypeSolver();
    reflectionTypeSolver = new ReflectionTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
  }

  /**
   * Creates a new JavaParserService with the needed TypeSolvers.
   *
   * @param sourcePath the path to the source code in the repository
   */
  public JavaParserService(final String sourcePath) {
    this.sourcePath = sourcePath;
    combinedTypeSolver = new CombinedTypeSolver();
    reflectionTypeSolver = new ReflectionTypeSolver();
    javaParserTypeSolver = new JavaParserTypeSolver(Path.of(sourcePath));
    combinedTypeSolver.add(reflectionTypeSolver);
    combinedTypeSolver.add(javaParserTypeSolver);
    javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
  }

  private FileDataHandler parse(final CompilationUnit compilationUnit, final String fileName) {
    final FileDataHandler data = new FileDataHandler(fileName);
    final MultiCollectorVisitor multiCollectorVisitor = new MultiCollectorVisitor();
    multiCollectorVisitor.visit(compilationUnit, data);
    return data;
  }

  /**
   * Resets the state of the JavaParserService, all cached values are cleared and the parser can be
   * reused for another task.
   */
  public void reset() {
    combinedTypeSolver = new CombinedTypeSolver();
    reflectionTypeSolver = new ReflectionTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    if (sourcePath != null) {
      javaParserTypeSolver = new JavaParserTypeSolver(sourcePath);
      combinedTypeSolver.add(javaParserTypeSolver);
    }
    javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
  }

  /**
   * Resets the JavaParserService but also resets the path to the source code.
   *
   * @param sourcePath the path to the source folder
   */
  public void reset(final String sourcePath) {
    this.sourcePath = sourcePath;
    reset();
  }


  /**
   * Tries to solve the given name with the attached TypeSolvers in the current Context.
   *
   * @param name the name of the type to solve
   * @return Empty, if not able, the FQN otherwise
   */
  public Optional<String> solveTypeInCurrentContext(final String name) {
    final SymbolReference<ResolvedReferenceTypeDeclaration> ref = combinedTypeSolver.tryToSolveType(
        name);
    if (ref.isSolved()) {
      return Optional.of(ref.getCorrespondingDeclaration().toString());
    }
    return Optional.empty();
  }

  private FileDataHandler parseAny(final String fileContent, final String fileName, final Path path)
      throws IOException {
    StaticJavaParser.getConfiguration().setSymbolResolver(this.javaSymbolSolver);
    final CompilationUnit compilationUnit;

    if (path == null) {
      compilationUnit = StaticJavaParser.parse(fileContent);
    } else {
      compilationUnit = StaticJavaParser.parse(path);
    }

    try {
      return parse(compilationUnit, fileName);
    } catch (NoSuchElementException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("NoSuchElementException: \n" + compilationUnit.toString());
      }
    } catch (NoSuchFieldError e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("NoSuchFieldError: \n" + compilationUnit.toString());
      }
    } catch (Exception | Error e) { // NOPMD
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(e.getClass().toString());
      }
    }
    return null;
  }

  /**
   * Parses file content.
   *
   * @param fileContent stringified java file
   */
  public FileDataHandler parseFileContent(final String fileContent, final String fileName) {
    try {
      return parseAny(fileContent, fileName, null);
    } catch (IOException e) {
      //   omit the IO Exception as the function can throw the exception only if path is not null
      return null;
    }
  }

  /**
   * Parses a file completely.
   *
   * @throws IOException Gets thrown if the file is not reachable
   */
  public FileDataHandler parseFile(final String pathToFile) throws IOException {
    final Path path = Path.of(pathToFile);
    return parseAny("", path.getFileName().toString(), path);
  }

}
