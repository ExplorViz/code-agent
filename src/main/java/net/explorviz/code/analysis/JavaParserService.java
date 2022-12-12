package net.explorviz.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.visitor.ClassNameVisitor;
import net.explorviz.code.analysis.visitor.LocVisitor;
import net.explorviz.code.proto.StructureFileEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service which handles the parsing of source code via JavaParser. Processes source code directory
 * once ${explorviz.watchservice.folder} upon application startup. Afterwards, listens to Vert.x
 * events with name "filechange" to only analyze a passed absolute filepath.
 */
@ApplicationScoped
public class JavaParserService {

  // private static final Logger LOGGER = LoggerFactory.getLogger(JavaParserService.class);

  @ConfigProperty(name = "explorviz.landscape.token")
  /* default */ String landscapeToken; // NOCS

  @ConfigProperty(name = "explorviz.landscape.secret")
  /* default */ String landscapeSecret; // NOCS

  private final VoidVisitor<List<String>> classNameVisitor = new ClassNameVisitor();
  private final GenericVisitorAdapter<Integer, Void> locCollector = new LocVisitor();
  // private final VoidVisitor<List<String>> inheritanceCollector = new InheritanceVisitor();
  // private final VoidVisitor<List<String>> implementedInterfacesCollector =
  // new ImplementedInterfaceVisitor();
  // private final VoidVisitor<List<String>> methodCallCollector = new MethocCallVisitor();
  // private final VoidVisitor<List<String>> importVisitor = new ImportVisitor();
  // private final GenericVisitorAdapter<String, String> packageCollector = new
  // PackageNameVisitor();


  /**
   * Analyzes a given stringified class, e.g., finds its fqn.
   *
   * @param stringifiedClass The stringified class that will be analyzed by the JavaParser.
   * @return List of StructureFileEvent. Often contains only one element, i.e., multiple elements
   *     indicate a nested class.
   * @throws IOException Throwed if the parsing of the stringified class fails.
   */
  public List<StructureFileEvent> processStringifiedClass(final String stringifiedClass)
      throws IOException {
    final CompilationUnit cu = StaticJavaParser.parse(stringifiedClass);

    final List<StructureFileEvent> returnValues = new ArrayList<>();

    // full text
    // loc
    // # of methods
    // # of variables

    // # of constructors
    // fqn of inheritances
    // fqn of implemented interfaces
    // fqn of imports
    // fqn of outgoing method calls

    final List<String> classNames = new ArrayList<>();
    this.classNameVisitor.visit(cu, classNames);
    for (final String className : classNames) {

      final int locWithoutComments = this.locCollector.visit(cu, null);

      final int numberOfMethods = cu.findAll(MethodDeclaration.class).size();

      final int numberOfFields = cu.findAll(FieldDeclaration.class).size();

      final StructureFileEvent.Builder builder = StructureFileEvent.newBuilder();

      builder.setFullyQualifiedOperationName(className);
      builder.setLandscapeToken(this.landscapeToken);
      builder.setLandscapeSecret(this.landscapeSecret);

      builder.putArguments("content-file", stringifiedClass);

      builder.putArguments("count-code-lines", String.valueOf(locWithoutComments));
      builder.putArguments("count-methods", String.valueOf(numberOfMethods));
      builder.putArguments("count-fields", String.valueOf(numberOfFields));

      returnValues.add(builder.build());
    }


    return returnValues;

  }

}
