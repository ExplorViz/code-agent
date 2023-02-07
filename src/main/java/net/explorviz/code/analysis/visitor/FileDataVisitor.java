package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.explorviz.code.analysis.handler.ConstructorDataHandler;
import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.handler.MethodDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor filling a FileData object with typical information in java files.
 */
public class FileDataVisitor extends VoidVisitorAdapter<FileDataHandler> { // NOPMD

  private static final Logger LOGGER = LoggerFactory.getLogger(FileDataVisitor.class);
  private static final String UNKNOWN = "UNKNOWN";
  private final GenericVisitorAdapter<Integer, FileDataHandler> nPathVisitor;
  // private final VoidVisitorAdapter<FileDataHandler> acPathVisitor;

  public FileDataVisitor(Optional<NPathVisitor> nPathVisitor,
                         Optional<ACPathVisitor> acPathVisitor) {
    this.nPathVisitor = nPathVisitor.isPresent() ? nPathVisitor.get() : new EmptyGenericVisitor();
    // this.acPathVisitor = acPathVisitor.isPresent() ? acPathVisitor.get() : new EmptyVoidVisitor();
  }

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
    final List<String> modifierList = new ArrayList<>();
    for (final Modifier modifier : n.getModifiers()) {
      modifierList.add(modifier.getKeyword().asString());
    }
    for (final VariableDeclarator declarator : n.getVariables()) {
      data.getCurrentClassData().addField(declarator.getNameAsString(),
          resolveFqn(declarator.getType(), data),
          modifierList);
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
    final String methodsFullyQualifiedName =
        data.getCurrentClassName() + "." + n.getNameAsString() + "#" + parameterHash(
            n.getParameters());
    final String returnType = resolveFqn(n.getType(), data);
    final MethodDataHandler method = data.getCurrentClassData()
        .addMethod(methodsFullyQualifiedName, returnType);
    for (final Modifier modifier : n.getModifiers()) {
      method.addModifier(modifier.getKeyword().asString());
    }
    for (final Parameter parameter : n.getParameters()) {
      method.addParameter(parameter.getNameAsString(), resolveFqn(parameter.getType(), data),
          parameter.getModifiers());
    }
    method.setLoc(getLoc(n));
    nPathVisitor.visit(n, data);
    super.visit(n, data);
  }

  @Override
  public void visit(final ConstructorDeclaration n, final FileDataHandler data) {
    final String constructorsFullyQualifiedName =
        data.getCurrentClassName() + "." + n.getNameAsString() + "#" + parameterHash(
            n.getParameters());
    final ConstructorDataHandler constructor = data.getCurrentClassData()
        .addConstructor(constructorsFullyQualifiedName);
    for (final Modifier modifier : n.getModifiers()) {
      constructor.addModifier(modifier.getKeyword().asString());
    }
    for (final Parameter parameter : n.getParameters()) {
      constructor.addParameter(parameter.getNameAsString(), resolveFqn(parameter.getType(), data),
          parameter.getModifiers());
    }
    constructor.setLoc(getLoc(n));
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

  // If FieldAccessExpr, then tight coupling
  @Override
  public void visit(MethodCallExpr n, FileDataHandler arg) {
    // System.out.println(n.getNameAsString());
    super.visit(n, arg);
  }

  @Override
  public void visit(ObjectCreationExpr n, FileDataHandler arg) {
    // System.out.println(n.toString());
    super.visit(n, arg);
  }

  @Override
  public void visit(ModuleDeclaration n, FileDataHandler arg) {
    // TODO NOT SUPPORTED
    super.visit(n, arg);
  }

  @Override
  public void visit(RecordDeclaration n, FileDataHandler arg) {
    // TODO NOT SUPPORTED
    super.visit(n, arg);
  }

  private String resolveFqn(final Type type, final FileDataHandler data) {
    try {
      final ResolvedType resolvedType = type.resolve();
      if (resolvedType.isReferenceType()) {
        return buildResolvedTypeFullDepthType(resolvedType.asReferenceType());
      } else {
        return type.toString();
      }
    } catch (UnsolvedSymbolException | IllegalStateException e) {
      return findFqnInImports(type, data);
    } catch (UnsupportedOperationException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "UnsupportedOperationException encountered, "
                + "reason currently unknown. Try to resolve but this may fail.");
      }
      // TODO what happens here? Error gets thrown but I don't know the reason...
      if (e.getMessage().contains("CorrespondingDeclaration")) {
        return findFqnInImports(type, data);
      }
      return findFqnInImports(type, data);
    }
  }

  private String buildResolvedTypeFullDepthType(final ResolvedReferenceType resolvedType) {
    final List<String> genericList = new ArrayList<>();
    for (final ResolvedType rt : resolvedType.typeParametersValues()) {
      if (rt.isReferenceType()) {
        genericList.add(buildResolvedTypeFullDepthType(rt.asReferenceType()));
      } else if (rt.isTypeVariable()) {
        genericList.add(rt.asTypeParameter().getName()); // TODO Does not work!
      } else {
        genericList.add(rt.toString());
      }
    }
    return resolvedType.asReferenceType().getQualifiedName() + typeListToGeneric(genericList);
  }

  /**
   * Returns the FQN for the type by simply comparing it with potential imports. If no import
   * matches the type, the type itself will be returned
   *
   * @param type the type of the Object
   * @return the fqn or the original type
   */
  private String findFqnInImports(final Type type, final FileDataHandler data) {  // NOPMD
    final List<String> imports = data.getImportNames();
    String attachedGenerics = "";
    if (type instanceof ClassOrInterfaceType) {
      final ClassOrInterfaceType classOrInterfaceType = type.asClassOrInterfaceType();
      if (classOrInterfaceType.getTypeArguments().isPresent()) {
        final List<String> typeList = new ArrayList<>();
        for (final Type localType : classOrInterfaceType.getTypeArguments().get()) {
          typeList.add(resolveFqn(localType, data));
        }
        attachedGenerics = typeListToGeneric(typeList);
      }
      for (final String importEntry : imports) {
        if (importEntry.endsWith(classOrInterfaceType.getName().asString())) {
          return importEntry + attachedGenerics;
        }
      }
    }
    // check imports
    for (final String importEntry : imports) {
      if (importEntry.endsWith(type.asString())) {
        return importEntry + attachedGenerics;
      }
    }

    if (LOGGER.isErrorEnabled()) {
      LOGGER.error("Unable to get FQN for <" + type.asString() + ">");
    }
    return type.asString() + attachedGenerics;
  }

  private String typeListToGeneric(final List<String> typeList) {
    if (typeList.isEmpty()) {
      return "";
    }
    final StringBuilder generics = new StringBuilder("<");
    for (int i = 0; i < typeList.size(); i++) {
      generics.append(typeList.get(i)).append(i + 1 == typeList.size() ? ">" : ", ");
    }
    return generics.toString();
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

  /**
   * Calculates the hash for a parameter list provided as String List.
   *
   * @param list a list of Types
   * @return the hash of the types as hexadecimal string
   */
  public static String parameterHash(final List<String> list) {
    return Integer.toHexString(list.hashCode());
  }

  /**
   * Calculates the hash for a parameter list provided as {@link NodeList}.
   *
   * @param parameterList a list of Parameters
   * @return the hash of the parameters as hexadecimal string
   */
  public static String parameterHash(final NodeList<Parameter> parameterList) {
    final List<String> tempList = new ArrayList<>();
    for (final Parameter parameter : parameterList) {
      tempList.add(parameter.getName().asString());
    }
    return Integer.toHexString(tempList.hashCode());
  }
}