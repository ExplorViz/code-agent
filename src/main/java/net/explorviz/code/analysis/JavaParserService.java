package net.explorviz.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.exceptions.DebugFileWriter;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.solver.CustomSolver;
import net.explorviz.code.analysis.visitor.MultiCollectorVisitor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser Object loads and parses .java files.
 */
@ApplicationScoped
public class JavaParserService {

  @ConfigProperty(name = "explorviz.gitanalysis.debug.filepath", defaultValue = "")
  /* default */ String debugFilePath;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.debug", defaultValue = "false")
  /* default */ boolean isDebugMode;  // NOCS


  public static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);

  private FileDataHandler parse(final CompilationUnit compilationUnit, final String fileName) {
    final FileDataHandler data = new FileDataHandler(fileName);
    final MultiCollectorVisitor multiCollectorVisitor = new MultiCollectorVisitor();
    try {
      multiCollectorVisitor.visit(compilationUnit, data);
    } catch (NoSuchElementException | NoSuchFieldError e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(e.getClass().getName() + " in " + fileName);
        System.out.println(e.getClass().getName() + " in " + fileName);
      }
      if (isDebugMode) {
        DebugFileWriter.saveDebugFile(debugFilePath, compilationUnit.toString(), fileName);
      }
      throw e;
    } catch (Exception | Error e) { // NOPMD
      if (isDebugMode) {
        DebugFileWriter.saveDebugFile(debugFilePath, compilationUnit.toString(), fileName);
      } else {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error(e.getClass().toString());
          System.out.println(e.getClass().toString());
        }
        throw e;
      }
      // LOGGER.error( e.getClass().toString() + ": \n" + compilationUnit.toString());
    }
    return data;
  }

  /**
   * @param fileContent stringified java file
   */
  public FileDataHandler fullParse(final String fileContent, final String fileName) {
    final TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    final CompilationUnit compilationUnit = StaticJavaParser.parse(fileContent);

    return parse(compilationUnit, fileName);

  }

  /**
   * Parses a test file completely.
   *
   * @throws IOException Gets thrown if the file is not reachable
   */
  public FileDataHandler fullParse(final String pathToFile) throws IOException {
    final Path path = Path.of(pathToFile);
    // final JavaParserTypeSolver javaParserTypeSolver = new JavaParserTypeSolver(
    //     "C:\\Users\\Julian\\projects\\Bachelor\\spring-petclinic\\src\\main\\java\\org\\springframework\\samples\\petclinic\\owner");
    // final ReflectionTypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    // // reflectionTypeSolver.setParent(javaParserTypeSolver);
    // // reflectionTypeSolver.setParent();
    // final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
    // // typeSolver.add(reflectionTypeSolver);
    // typeSolver.add(javaParserTypeSolver);
    // if (true) {
    //   return null;
    // }
    final CustomSolver customSolver = new CustomSolver(
        "C:\\Users\\Julian\\projects\\Bachelor\\spring-petclinic\\src\\main\\java\\org\\springframework\\samples\\petclinic\\owner");
    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(customSolver);
    // final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    try {
      // System.out.println(reflectionTypeSolver.solveType("Owner").getQualifiedName());
    } catch (Exception e) {
      // System.out.println("Catch!");
      // System.out.println(typeSolver.solveType("Owner").getQualifiedName());
    }
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    final CompilationUnit compilationUnit = StaticJavaParser.parse(path);

    return parse(compilationUnit, path.getFileName().toString());
  }

}
