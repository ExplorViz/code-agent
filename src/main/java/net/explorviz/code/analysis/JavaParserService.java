package net.explorviz.code.analysis;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.SourceRoot.Callback;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.code.analysis.visitor.ClassNameVisitor;
import net.explorviz.code.analysis.visitor.ImplementedInterfaceVisitor;
import net.explorviz.code.analysis.visitor.ImportVisitor;
import net.explorviz.code.analysis.visitor.InheritanceVisitor;
import net.explorviz.code.analysis.visitor.LocVisitor;
import net.explorviz.code.analysis.visitor.MethocCallVisitor;
import net.explorviz.code.analysis.visitor.MethodVisitor;
import net.explorviz.code.analysis.visitor.PackageNameVisitor;
import net.explorviz.code.proto.StructureCreateEvent;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureModifyEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which handles the parsing of source code via JavaParser. Processes source code directory
 * once ${explorviz.watchservice.folder} upon application startup. Afterwards, listens to Vert.x
 * events with name "filechange" to only analyze a passed absolute filepath.
 */
@ApplicationScoped
public class JavaParserService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);

  private final ParserConfiguration config;

  private final String folderPath;

  private final VoidVisitor<List<String>> classNameVisitor = new ClassNameVisitor();
  private final GenericVisitorAdapter<Integer, Integer> locCollector = new LocVisitor();
  private final VoidVisitor<List<String>> inheritanceCollector = new InheritanceVisitor();
  private final VoidVisitor<List<String>> implementedInterfacesCollector =
      new ImplementedInterfaceVisitor();
  private final VoidVisitor<List<String>> methodCollector = new MethodVisitor();
  private final VoidVisitor<List<String>> methodCallCollector = new MethocCallVisitor();
  private final VoidVisitor<List<String>> importVisitor = new ImportVisitor();
  private final GenericVisitorAdapter<String, String> packageCollector = new PackageNameVisitor();

  @GrpcClient("StructureEventService")
  StructureEventService structureEventService;

  /**
   * Constructor for this class. Injects {@link ConfigProperty} ${explorviz.watchservice.folder} for
   * the initial analysis of the top level folder path to the source code.
   *
   * @param folderPath Absolute folder path used to setup the type solver and configure the initial
   *        analysis of the source code.
   */
  @Inject
  public JavaParserService(
      @ConfigProperty(name = "explorviz.watchservice.folder") final String folderPath) {

    this.folderPath = folderPath;

    final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(),
        new JavaParserTypeSolver(this.folderPath));

    final JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

    this.config = new ParserConfiguration().setStoreTokens(true).setSymbolResolver(symbolSolver);

    StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
  }

  /**
   * This method listens to the Vert.x event "filechange" and will analyze a given absolute file
   * path with the JavaParser.
   *
   * @param absoluteFilePath Absolute file path that will be analyzed by the JavaParser.
   * @throws IOException Throwed if the parsing of the absoluteFilePath fails.
   */
  @ConsumeEvent("filechange")
  public void processFile(final String absoluteFilePath) throws IOException {
    final CompilationUnit cu = StaticJavaParser.parse(Paths.get(absoluteFilePath));

    final List<String> classNames = new ArrayList<>();
    System.out.println("Class names:");

    // print fqn
    JavaParserService.this.classNameVisitor.visit(cu, classNames);
    for (final String className : classNames) {
      System.out.println(className);
      final StructureModifyEvent event =
          StructureModifyEvent.newBuilder().setClassName(className).build();
      JavaParserService.this.structureEventService.sendModifyEvent(event);
    }

  }

  /**
   * Recursive source code analysis with JavaParser of a given folder.
   *
   * @param absoluteFolderPath Absolute folder path that will be analyzed by the JavaParser.
   * @throws IOException Throwed if the parsing of the absoluteFolderPath fails.
   */
  public void processFolder(final String absoluteFolderPath) throws IOException {
    final Path pathToSource = Paths.get(absoluteFolderPath);
    final SourceRoot sourceRoot = new SourceRoot(pathToSource);

    final List<String> className = new ArrayList<>();
    final List<String> superClassNames = new ArrayList<>();
    final List<String> implementedInterfacesClassNames = new ArrayList<>();
    final List<String> methodsOfClass = new ArrayList<>();
    final List<String> calledMethodsInClass = new ArrayList<>();
    final List<String> importNames = new ArrayList<>();

    sourceRoot.parse("", this.config, new Callback() {

      @Override
      public Result process(final Path localPath, final Path absolutePath,
          final ParseResult<CompilationUnit> result) {

        if (result.isSuccessful() && result.getResult().isPresent()) {
          final CompilationUnit cu = result.getResult().get();

          System.out.println("Class names:");

          // print fqn
          JavaParserService.this.classNameVisitor.visit(cu, className);
          for (final String className : className) {
            System.out.println(className);
            final StructureCreateEvent event =
                StructureCreateEvent.newBuilder().setClassName(className).build();
            JavaParserService.this.structureEventService.sendCreateEvent(event).await()
                .indefinitely();
          }

          className.clear();

          /*
           * System.out.println("Package:");
           * System.out.println(JavaParserService.this.packageCollector.visit(cu, ""));
           *
           * System.out.println("LoC:");
           * System.out.println(JavaParserService.this.locCollector.visit(cu, 0));
           *
           *
           *
           * System.out.println("Imports:"); JavaParserService.this.importVisitor.visit(cu,
           * importNames); importNames.forEach(n -> System.out.println(n));
           *
           * System.out.println("Super classes:");
           *
           * JavaParserService.this.inheritanceCollector.visit(cu, superClassNames);
           * superClassNames.forEach( n ->
           * System.out.println(FqnCalculator.calculateFqnBasedOnImport(importNames, n)));
           *
           * System.out.println("Implemented interfaces:");
           *
           * JavaParserService.this.implementedInterfacesCollector.visit(cu,
           * implementedInterfacesClassNames); implementedInterfacesClassNames.forEach( n ->
           * System.out.println(FqnCalculator.calculateFqnBasedOnImport(importNames, n)));
           *
           * System.out.println("Contained Methods:");
           *
           * JavaParserService.this.methodCollector.visit(cu, methodsOfClass);
           * methodsOfClass.forEach(n -> System.out.println(n));
           *
           * System.out.println("Called methods:");
           *
           * JavaParserService.this.methodCallCollector.visit(cu, calledMethodsInClass);
           * calledMethodsInClass.forEach(n -> System.out.println(n));
           *
           * importNames.clear(); superClassNames.clear(); implementedInterfacesClassNames.clear();
           * methodsOfClass.clear(); calledMethodsInClass.clear();
           *
           * System.out.println("");
           */
        }
        return Result.DONT_SAVE;
      }

    });
  }

  /* default */ void onStart(@Observes final StartupEvent ev) throws IOException {
    LOGGER.debug("Starting initial analysis of the source code for directory {} ...",
        this.folderPath);
    this.processFolder(this.folderPath);
  }

}
