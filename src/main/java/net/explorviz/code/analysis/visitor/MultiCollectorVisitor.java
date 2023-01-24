package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.List;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MethodDataHandler;
import static net.explorviz.code.analysis.types.JavaTypes.built_ins;
import static net.explorviz.code.analysis.types.JavaTypes.primitives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor filling a FileData object.
 */
public class MultiCollectorVisitor extends VoidVisitorAdapter<FileDataHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectorVisitor.class);
  // private final TypeSolver solver;
  //
  // public MultiCollectorVisitor() {
  //   this.solver = null;
  // }
  //
  // public MultiCollectorVisitor(TypeSolver solver) {
  //   this.solver = solver;
  // }

  @Override
  public void visit(final PackageDeclaration n, final FileDataHandler data) {
    data.setPackageName(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final ImportDeclaration n, final FileDataHandler data) {
    data.addImport(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(EnumDeclaration n, FileDataHandler data) {
    data.enterClass(n.getFullyQualifiedName().orElse("UNKNOWN"));
    data.getCurrentClassData().setLoc(getLoc(n));
    data.getCurrentClassData().setIsEnum();
    for (final Modifier modifier : n.getModifiers()) {
      data.getCurrentClassData().addModifier(modifier.getKeyword().asString());
    }
    super.visit(n, data);
    data.leaveClass();
  }

  @Override
  public void visit(final FieldDeclaration n, final FileDataHandler data) {
    // System.out.println(n.getVariables());
    for (final VariableDeclarator declarator : n.getVariables()) {
      data.getCurrentClassData().addField(declarator.getNameAsString());
    }
    super.visit(n, data);
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final FileDataHandler data) {

    data.enterClass(n.getFullyQualifiedName().orElse("UNKNOWN"));
    data.getCurrentClassData().setLoc(getLoc(n));

    if (n.isInterface()) {
      data.getCurrentClassData().setIsInterface();
    } else if (n.isAbstract()) {
      data.getCurrentClassData().setIsAbstractClass();
    } else {
      data.getCurrentClassData().setIsClass();
    }

    for (final Modifier modifier : n.getModifiers()) {
      data.getCurrentClassData().addModifier(modifier.getKeyword().asString());
    }

    // TODO: more than one if interface?
    if (n.getExtendedTypes().getFirst().isPresent()) {
      try {
        final String fqn = n.getExtendedTypes().getFirst().get().resolve().asReferenceType()
            .getQualifiedName();
        data.getCurrentClassData().setSuperClass(fqn);
      } catch (UnsolvedSymbolException | IllegalStateException e) {
        System.out.println("Catch 93");
        data.getCurrentClassData()
            .setSuperClass(findFqnInImports(n.getExtendedTypes().getFirst().get().getNameAsString(),
                data.getImportNames()));
      }
      // } catch (IllegalStateException e) {
      //   data.getCurrentClassData()
      //       .setSuperClass(findFqnInImports(n.getExtendedTypes()
      //       .getFirst().get().getNameAsString(),
      //           data.getImportNames()));
      // }
    }

    for (int i = 0; i < n.getImplementedTypes().size(); i++) {
      try {
        final String fqn = n.getImplementedTypes().get(i).getElementType().resolve()
            .asReferenceType()
            .getQualifiedName();
        data.getCurrentClassData().addImplementedInterface(fqn);
      } catch (UnsolvedSymbolException | IllegalStateException e) {
        System.out.println("113");
        data.getCurrentClassData()
            .addImplementedInterface(
                findFqnInImports(n.getImplementedTypes().get(i).getNameAsString(),
                    data.getImportNames()));
      }
      // } catch (IllegalStateException e) {
      //   data.getCurrentClassData()
      //       .addImplementedInterface(
      //           findFqnInImports(n.getImplementedTypes().get(i).getNameAsString(),
      //               data.getImportNames()));
      // }
    }

    super.visit(n, data);
    data.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final FileDataHandler data) {
    final String methodsFullyQualifiedName = data.getCurrentClassName() + "." + n.getNameAsString();
    final String returnType = n.getType().asString();
    final MethodDataHandler method = data.getCurrentClassData()
        .addMethod(methodsFullyQualifiedName, returnType);
    for (final Modifier modifier : n.getModifiers()) {
      method.addModifier(modifier.getKeyword().toString());
    }
    for (final Parameter parameter : n.getParameters()) {
      try {
        // if (parameter.getType().asString().equals("Owner")) {
        //   Type t = parameter.getType();
        //   final ResolvedType type = t.resolve();
        // }
        System.out.println(parameter.getType().toString());
        final ResolvedType type = parameter.getType().resolve();
        if (type.isReferenceType()) {
          method.addParameter(type.asReferenceType().getQualifiedName());
        } else {
          method.addParameter(parameter.getType().toString());
        }
      } catch (UnsolvedSymbolException | IllegalStateException e) {
        System.out.println("Catch 150 - " + e.getClass().toString());
        method.addParameter(findFqnInImports(parameter.getType().asString(),
            data.getImportNames()));
        // Only used if no resolver present
      } catch (UnsupportedOperationException e) {
        System.out.println("Catch 155");
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(
              "UnsupportedOperationException encountered, "
                  + "not sure why this happens, resolved for now.");
        }
        if (e.getMessage().contains("CorrespondingDeclaration")) {
          method.addParameter(findFqnInImports(parameter.getType().asString(),
              data.getImportNames()));
        }
        // } catch (NoSuchFieldError e) {
        //   System.out.println(parameter.getType().asString());
      }
    }
    super.visit(n, data);
  }

  @Override
  public void visit(final ConstructorDeclaration n, final FileDataHandler data) {
    data.getCurrentClassData().addConstructor(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(EnumConstantDeclaration n, FileDataHandler data) {
    data.getCurrentClassData().addEnumConstant(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final CompilationUnit n, final FileDataHandler data) {
    data.setLoc(getLoc(n));
    super.visit(n, data);
  }

  /**
   * Returns the FQN for the type by simply comparing it with potential imports. If no import
   * matches the type, the type itself will be returned
   *
   * @param type the type of the Object
   * @return the fqn or the original type
   */
  private String findFqnInImports(final String type, final List<String> imports) {
    // check if Primitive
    for (final String primitive : primitives) {
      if (type.equals(primitive)) {
        return type;
      }
    }
    // check imports
    for (final String importEntry : imports) {
      if (importEntry.endsWith(type)) {
        return importEntry;
      }
    }

    // check build in types from java.lang
    for (final String built_in : built_ins) {
      if (type.equals(built_in)) {
        return "java.lang." + type;
      }
    }
    if (LOGGER.isErrorEnabled()) {
      LOGGER.error("Unable to get FQN for <" + type + ">");
      System.out.println("Unable to get FQN for <" + type + ">");
    }
    return type;
  }


  private int getLoc(final Node node) {
    if (node.getRange().isPresent()) {
      int linesOfComments = 0;
      for (final Comment commentNode : node.getAllContainedComments()) {
        if (commentNode.getRange().isPresent()) {
          linesOfComments += commentNode.getRange().get().getLineCount();
        }
      }
      return node.getRange().get().getLineCount() - linesOfComments;
    }
    if (LOGGER.isErrorEnabled()) {
      LOGGER.error("Getting the lines of code failed!");
    }
    return 0;
  }
}