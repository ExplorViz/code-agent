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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.List;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MethodDataHandler;
import net.explorviz.code.analysis.types.JavaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor filling a FileData object.
 */
public class MultiCollectorVisitor extends VoidVisitorAdapter<FileDataHandler> { // NOPMD

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectorVisitor.class);
  private static final String UNKNOWN = "UNKNOWN";

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
  public void visit(final EnumDeclaration n, final FileDataHandler data) {
    data.enterClass(n.getFullyQualifiedName().orElse(UNKNOWN));
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
  public void visit(final ClassOrInterfaceDeclaration n, final FileDataHandler data) { // NOPMD

    data.enterClass(n.getFullyQualifiedName().orElse(UNKNOWN));
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

    for (final ClassOrInterfaceType classOrInterfaceType : n.getExtendedTypes()) {
      final String fqn = resolveFqn(classOrInterfaceType, data);
      if (data.getCurrentClassData().isClass() || data.getCurrentClassData().isAbstractClass()) {
        data.getCurrentClassData().setSuperClass(fqn);
      } else if (data.getCurrentClassData().isInterface()) {
        data.getCurrentClassData().addImplementedInterface(fqn);
      } else {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error(
              "Unexpected Error, Declaration is neither Interface, AbstractClass nor Class but"
                  + " has Interface(s) attached to it");
        }
      }
    }

    for (final ClassOrInterfaceType classOrInterfaceType : n.getImplementedTypes()) {
      data.getCurrentClassData().addImplementedInterface(resolveFqn(classOrInterfaceType, data));
    }

    super.visit(n, data);
    data.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final FileDataHandler data) {
    final String methodsFullyQualifiedName = data.getCurrentClassName() + "." + n.getNameAsString();
    // TODO get fqn here too
    final String returnType = resolveFqn(n.getType(), data);
    final MethodDataHandler method = data.getCurrentClassData()
        .addMethod(methodsFullyQualifiedName, returnType);
    for (final Modifier modifier : n.getModifiers()) {
      method.addModifier(modifier.getKeyword().toString());
    }
    for (final Parameter parameter : n.getParameters()) {
      method.addParameter(resolveFqn(parameter.getType(), data));
    }
    super.visit(n, data);
  }

  @Override
  public void visit(final ConstructorDeclaration n, final FileDataHandler data) {
    data.getCurrentClassData().addConstructor(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final EnumConstantDeclaration n, final FileDataHandler data) {
    data.getCurrentClassData().addEnumConstant(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final CompilationUnit n, final FileDataHandler data) {
    data.setLoc(getLoc(n));
    super.visit(n, data);
  }

  private String resolveFqn(final Type type, final FileDataHandler data) {
    // if (data.getFileName().endsWith("NamedEntity.java") || (
    //     data.getFileName().endsWith("PetController.java") && "Pet".equals(type.asString()))) {
    //   //  && type.toString().equals("<BaseEntity>")
    //   int i = 1;
    // }
    try {
      final ResolvedType resolvedType = type.resolve();
      if (resolvedType.isReferenceType()) {
        return resolvedType.asReferenceType().getQualifiedName();
      } else {
        return type.toString();
      }
    } catch (UnsolvedSymbolException | IllegalStateException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("UnresolvedSymbolException " + type.asString());
      }
      return findFqnInImports(type.asString(), data.getImportNames());
      // Only used if no resolver present
    } catch (UnsupportedOperationException e) {
      // System.out.println("Catch 155");
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "UnsupportedOperationException encountered, "
                + "not sure why this happens, resolved for now.");
      }
      // TODO why this? Still some debugging code?
      if (e.getMessage().contains("CorrespondingDeclaration")) {
        return findFqnInImports(type.asString(), data.getImportNames());
      }
      return findFqnInImports(type.asString(), data.getImportNames());
    }
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
    for (final String primitive : JavaTypes.PRIMITIVES) {
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
    for (final String builtIn : JavaTypes.BUILT_INS) {
      if (type.equals(builtIn)) {
        return "java.lang." + type;
      }
    }
    if (LOGGER.isErrorEnabled()) {
      LOGGER.error("Unable to get FQN for <" + type + ">");
    }
    // System.out.println("Unable to get FQN for <" + type + ">");
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