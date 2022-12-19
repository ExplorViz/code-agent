package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.List;
import net.explorviz.code.analysis.DataObjects.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MultiCollectorVisitor extends VoidVisitorAdapter<FileData> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectorVisitor.class);

  @Override
  public void visit(PackageDeclaration n, FileData data) {
    data.packageName = n.getNameAsString();
    super.visit(n, data);
  }

  @Override
  public void visit(ImportDeclaration n, FileData data) {
    data.importNames.add(n.getNameAsString());
    super.visit(n, data);
  }

  @Override
  public void visit(FieldDeclaration n, FileData data) {
    // System.out.println(n.getVariables());
    for (VariableDeclarator declarator : n.getVariables()) {
      data.getCurrentClassData().addField(declarator.getNameAsString());
    }
    super.visit(n, data);
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n,
                    final FileData data) {
    // TODO: split class and Interface here
    data.enterClass(n.getFullyQualifiedName().orElse("UNKNOWN"));
    if (n.getExtendedTypes().getFirst().isPresent()) {
      try {
        String fqn = n.getExtendedTypes().getFirst().get().resolve().asReferenceType()
            .getQualifiedName();
        data.getCurrentClassData().setSuperClass(fqn);
      } catch (UnsolvedSymbolException unsolvedSymbolException) {
        data.getCurrentClassData()
            .setSuperClass(findFqnInImports(n.getExtendedTypes().getFirst().get().getNameAsString(),
                data.importNames));
      }
    }

    if (n.getImplementedTypes().size() >= 1) {
      for (int i = 0; i < n.getImplementedTypes().size(); i++) {
        try {
          String fqn = n.getImplementedTypes().get(i).getElementType().resolve().asReferenceType()
              .getQualifiedName();
          data.getCurrentClassData().addImplementedInterface(fqn);
        } catch (UnsolvedSymbolException unsolvedSymbolException) {
          data.getCurrentClassData()
              .addImplementedInterface(
                  findFqnInImports(n.getImplementedTypes().get(i).getNameAsString(),
                      data.importNames));

        }
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
  private String findFqnInImports(String type, List<String> imports) {
    for (String importEntry : imports) {
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