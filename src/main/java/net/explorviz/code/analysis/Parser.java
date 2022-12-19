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
import java.nio.file.Files;
import java.nio.file.Path;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.DataObjects.FileData;
import net.explorviz.code.analysis.visitor.MultiCollectorVisitor;


@ApplicationScoped
public class Parser {

  private void parse(CompilationUnit compilationUnit) {
    final FileData data = new FileData();
    MultiCollectorVisitor multiCollectorVisitor = new MultiCollectorVisitor();
    multiCollectorVisitor.visit(compilationUnit, data);
    System.out.println(data);
  }

  public void fullParse(String fileContent) {
    TypeSolver typeSolver = new CombinedTypeSolver(
        new ReflectionTypeSolver(),
        new JavaParserTypeSolver(
            new File("C:\\Users\\Julian\\projects\\Bachelor\\Fooling\\src\\main\\java"))
    );
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    // StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    CompilationUnit compilationUnit = StaticJavaParser.parse(fileContent);
    parse(compilationUnit);

  }

  public void fullParse() throws IOException {
    Path path = Path.of(
        "C:\\Users\\Julian\\projects\\Bachelor\\Fooling\\src\\main\\java\\org\\example\\Main.java");
    final String classContent = Files.readString(path);
    TypeSolver typeSolver = new CombinedTypeSolver(
        new ReflectionTypeSolver(),
        new JavaParserTypeSolver(
            new File("C:\\Users\\Julian\\projects\\Bachelor\\Fooling\\src\\main\\java"))
    );
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    // StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    CompilationUnit compilationUnit = StaticJavaParser.parse(path);
    parse(compilationUnit);
  }

}
