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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.exceptions.DebugFileWriter;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.visitor.ACPathVisitor;
import net.explorviz.code.analysis.visitor.FileDataVisitor;
import net.explorviz.code.analysis.visitor.NPathVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser Object loads and parses .java files.
 */
@ApplicationScoped
public class JavaParserService {
  public static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);

  private List<String> sourcePaths;
  private JavaSymbolSolver javaSymbolSolver;
  private CombinedTypeSolver combinedTypeSolver;
  private ReflectionTypeSolver reflectionTypeSolver;

  /**
   * Creates a new JavaParserService without any TypeSolvers, call {@link JavaParserService#reset()}
   * or {@link JavaParserService#reset(List)} to get a valid JavaParserService.
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
   * @param sourcePaths the list of paths to the source code in the repository
   */
  public JavaParserService(final List<String> sourcePaths) {
    this.sourcePaths = sourcePaths;
    combinedTypeSolver = new CombinedTypeSolver();
    reflectionTypeSolver = new ReflectionTypeSolver();
    combinedTypeSolver.add(reflectionTypeSolver);
    for (final String path : sourcePaths) {
      combinedTypeSolver.add(new JavaParserTypeSolver(Path.of(path)));  // NOPMD
    }
    javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
  }

  /**
   * Creates a new JavaParserService with the needed TypeSolvers.
   *
   * @param sourcePath the path to the source code in the repository
   */
  public JavaParserService(final String sourcePath) {
    this(Collections.singletonList(sourcePath));
  }

  private FileDataHandler parse(final CompilationUnit compilationUnit, final String fileName,
                                boolean calculateMetrics) {
    final FileDataHandler data = new FileDataHandler(fileName);
    final FileDataVisitor multiCollectorVisitor;
    if (calculateMetrics) {
      multiCollectorVisitor = new FileDataVisitor(Optional.of(new NPathVisitor()),
          Optional.of(new ACPathVisitor()));
    } else {
      multiCollectorVisitor = new FileDataVisitor(Optional.empty(), Optional.empty());
    }
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
    for (final String path : this.sourcePaths) {
      combinedTypeSolver.add(new JavaParserTypeSolver(path)); // NOPMD
    }
    javaSymbolSolver = new JavaSymbolSolver(combinedTypeSolver);
  }

  /**
   * Resets the JavaParserService but also resets the paths to the source code.
   *
   * @param sourcePaths the paths to the source folders
   */
  public void reset(final List<String> sourcePaths) {
    this.sourcePaths = sourcePaths;
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

  private FileDataHandler parseAny(final String fileContent, final String fileName, final Path path,
                                   boolean calculateMetrics)
      throws IOException {
    StaticJavaParser.getConfiguration().setSymbolResolver(this.javaSymbolSolver);
    final CompilationUnit compilationUnit;

    if (path == null) {
      compilationUnit = StaticJavaParser.parse(fileContent);
    } else {
      compilationUnit = StaticJavaParser.parse(path);
    }

    // DEBUG CODE
    DebugFileWriter.saveAstAsYaml(compilationUnit, "/logs/quarkus/debug.yaml");

    try {
      return parse(compilationUnit, fileName, calculateMetrics);
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
  public FileDataHandler parseFileContent(final String fileContent, final String fileName,
                                          boolean calculateMetrics) {
    try {
      return parseAny(fileContent, fileName, null, calculateMetrics);
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
  public FileDataHandler parseFile(final String pathToFile, boolean calculateMetrics)
      throws IOException {
    final Path path = Path.of(pathToFile);
    return parseAny("", path.getFileName().toString(), path, calculateMetrics);
  }

}
