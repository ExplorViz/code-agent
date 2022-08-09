package net.explorviz.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import io.quarkus.grpc.GrpcClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.visitor.ClassNameVisitor;
import net.explorviz.code.proto.StructureEventService;
import net.explorviz.code.proto.StructureFileEvent;
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

  @GrpcClient("StructureEventService")
  /* default */ StructureEventService structureEventService; // NOCS

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeToken; // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecret; // NOCS

  private final VoidVisitor<List<String>> classNameVisitor = new ClassNameVisitor();
  // private final GenericVisitorAdapter<Integer, Integer> locCollector = new LocVisitor();
  // private final VoidVisitor<List<String>> inheritanceCollector = new InheritanceVisitor();
  // private final VoidVisitor<List<String>> implementedInterfacesCollector =
  // new ImplementedInterfaceVisitor();
  // private final VoidVisitor<List<String>> methodCollector = new MethodVisitor();
  // private final VoidVisitor<List<String>> methodCallCollector = new MethocCallVisitor();
  // private final VoidVisitor<List<String>> importVisitor = new ImportVisitor();
  // private final GenericVisitorAdapter<String, String> packageCollector = new
  // PackageNameVisitor();



  /**
   * This method listens to the Vert.x event "filechange" and will analyze a given absolute file
   * path with the JavaParser.
   *
   * @param absoluteFilePath Absolute file path that will be analyzed by the JavaParser.
   * @throws IOException Throwed if the parsing of the absoluteFilePath fails.
   */
  public void processFile(final String absoluteFilePath) throws IOException {
    final CompilationUnit cu = StaticJavaParser.parse(Paths.get(absoluteFilePath));

    final List<String> classNames = new ArrayList<>();
    LOGGER.debug("Class names:"); // NOCS

    // print fqn
    this.classNameVisitor.visit(cu, classNames);
    for (final String className : classNames) {
      LOGGER.debug("{}", className);
      final StructureFileEvent event = StructureFileEvent.newBuilder()
          .setFullyQualifiedOperationName(className).setLandscapeToken(this.landscapeToken)
          .setLandscapeSecret(this.landscapeSecret).build();
      this.structureEventService.sendStructureFileEvent(event).onItem()
          .invoke(() -> LOGGER.debug("12ALEX Done")).onCancellation()
          .invoke(() -> LOGGER.error("12ALEX Cancel")).onFailure()
          .invoke(t -> LOGGER.debug("12ALEX Failure, {}", t)).await().indefinitely();
    }

  }

}
