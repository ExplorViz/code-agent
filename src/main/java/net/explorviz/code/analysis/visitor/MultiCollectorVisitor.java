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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MethodDataHandler;
import net.explorviz.code.analysis.solver.TypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor filling a FileData object.
 */
public class MultiCollectorVisitor extends VoidVisitorAdapter<FileDataHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectorVisitor.class);
  private final TypeSolver solver;

  public MultiCollectorVisitor(TypeSolver solver) {
    this.solver = solver;
  }

  @Override
  public void visit(final PackageDeclaration n, final FileDataHandler data) {
    data.setPackageName(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final ImportDeclaration n, final FileDataHandler data) {
    data.addImport(n.getNameAsString());
    solver.addImport(n.getNameAsString());
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
    // for (int i = 0; i < n.getExtendedTypes().getFirst().isPresent()) {
    for (ClassOrInterfaceType type : n.getExtendedTypes()) {
      try {
        final String fqn = solver.solveType(type);
        if (data.getCurrentClassData().isClass() || data.getCurrentClassData().isAbstractClass()) {
          data.getCurrentClassData().setSuperClass(fqn);
        } else if (data.getCurrentClassData().isInterface()) {
          data.getCurrentClassData().addImplementedInterface(fqn);
        } else {
          System.out.println("No Interface, Class, or AbastractClass");
          if (LOGGER.isErrorEnabled()) {
            LOGGER.error(
                "Unexpected Error, Declaration is neither Interface, AbstractClass nor Class but"
                    + " has Interface(s) attached to it");
          }
        }
      } catch (UnsolvedTypeException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(e.getMessage());
          System.out.println(e.getMessage());
        }
        if (data.getCurrentClassData().isClass() || data.getCurrentClassData().isAbstractClass()) {
          data.getCurrentClassData().setSuperClass(type.asString());
        } else if (data.getCurrentClassData().isInterface()) {
          data.getCurrentClassData().addImplementedInterface(type.asString());
        } else {
          System.out.println("No Interface, Class, or AbastractClass");
          if (LOGGER.isErrorEnabled()) {
            LOGGER.error(
                "Unexpected Error, Declaration is neither Interface, AbstractClass nor Class but"
                    + " has Interface(s) attached to it");
          }
        }
      }
    }

    for (ClassOrInterfaceType type : n.getImplementedTypes()) {
      try {
        final String fqn = solver.solveType(type);
        data.getCurrentClassData().addImplementedInterface(fqn);
      } catch (UnsolvedTypeException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(e.getMessage());
          System.out.println(e.getMessage());
        }
        data.getCurrentClassData()
            .addImplementedInterface(type.asString());
      }
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
        String fqn = solver.solveType(parameter.getType());
        method.addParameter(fqn);
      } catch (UnsolvedTypeException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(e.getMessage());
          System.out.println(e.getMessage());
        }
        method.addParameter(parameter.getType().asString());
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