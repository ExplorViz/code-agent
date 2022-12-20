package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.List;
import net.explorviz.code.analysis.dataobjects.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor filling a FileData object.
 */
public class MultiCollectorVisitor extends VoidVisitorAdapter<FileData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectorVisitor.class);

  @Override
  public void visit(final PackageDeclaration n, final FileData data) {
    data.setPackageName(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final ImportDeclaration n, final FileData data) {
    data.addImport(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final FieldDeclaration n, final FileData data) {
    // System.out.println(n.getVariables());
    for (final VariableDeclarator declarator : n.getVariables()) {
      data.getCurrentClassData().addField(declarator.getNameAsString());
    }
    super.visit(n, data);
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n,
                    final FileData data) {

    data.enterClass(n.getFullyQualifiedName().orElse("UNKNOWN"));
    // data.getCurrentClassData().

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

    if (n.getExtendedTypes().getFirst().isPresent()) {
      try {
        final String fqn = n.getExtendedTypes().getFirst().get().resolve().asReferenceType()
            .getQualifiedName();
        data.getCurrentClassData().setSuperClass(fqn);
      } catch (UnsolvedSymbolException unsolvedSymbolException) {
        data.getCurrentClassData()
            .setSuperClass(findFqnInImports(n.getExtendedTypes().getFirst().get().getNameAsString(),
                data.getImportNames()));
      }
    }


    for (int i = 0; i < n.getImplementedTypes().size(); i++) {
      try {
        final String fqn = n.getImplementedTypes().get(i).getElementType().resolve()
            .asReferenceType()
            .getQualifiedName();
        data.getCurrentClassData().addImplementedInterface(fqn);
      } catch (UnsolvedSymbolException unsolvedSymbolException) {
        data.getCurrentClassData()
            .addImplementedInterface(
                findFqnInImports(n.getImplementedTypes().get(i).getNameAsString(),
                    data.getImportNames()));

      }
    }


    super.visit(n, data);
    data.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final FileData data) {
    data.getCurrentClassData().addMethod(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(final ConstructorDeclaration n, final FileData data) {
    data.getCurrentClassData().addConstructor(n.getNameAsString());
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
    for (final String importEntry : imports) {
      if (importEntry.endsWith(type)) {
        return importEntry;
      }
    }
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(
          "Unable to get FQN for <" + type + ">");
    }
    return type;
  }
}