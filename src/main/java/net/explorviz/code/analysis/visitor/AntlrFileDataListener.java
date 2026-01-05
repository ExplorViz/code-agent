package net.explorviz.code.analysis.visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.explorviz.code.analysis.antlr.generated.Java20Parser;
import net.explorviz.code.analysis.antlr.generated.Java20ParserBaseListener;
import net.explorviz.code.analysis.handler.JavaFileDataHandler;
import net.explorviz.code.analysis.handler.MethodDataHandler;
import net.explorviz.code.analysis.types.Verification;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ANTLR Listener-based implementation for extracting file data from Java source code.
 */
public class AntlrFileDataListener extends Java20ParserBaseListener {

  public static final String FILE_SIZE = "size";
  public static final String LOC = "loc";
  public static final String CLOC = "cloc";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(AntlrFileDataListener.class);

  private final JavaFileDataHandler fileDataHandler;
  private final boolean wildcardImportProperty;
  private int wildcardImportCount;
  private String wildcardImport;
  private String currentPackage = "";

  public AntlrFileDataListener(final JavaFileDataHandler fileDataHandler,
      final boolean wildcardImportProperty) {
    this.fileDataHandler = fileDataHandler;
    this.wildcardImportProperty = wildcardImportProperty;
    this.wildcardImportCount = 0;
    this.wildcardImport = null;
  }

  @Override
  public void enterCompilationUnit(final Java20Parser.CompilationUnitContext ctx) {
    // Calculate LOC and CLOC
    final int loc = getLoc(ctx);
    final int cloc = getCloc(ctx);
    
    fileDataHandler.addMetric(LOC, String.valueOf(loc));
    fileDataHandler.addMetric(CLOC, String.valueOf(cloc));
    
    LOGGER.atTrace()
        .addArgument(fileDataHandler.getFileName())
        .addArgument(loc)
        .log("{} - LOC: {}");
  }

  @Override
  public void enterPackageDeclaration(final Java20Parser.PackageDeclarationContext ctx) {
    // Package format: 'package' identifier ('.' identifier)* ';'
    final List<Java20Parser.IdentifierContext> identifiers = ctx.identifier();
    if (identifiers != null && !identifiers.isEmpty()) {
      currentPackage = identifiers.stream()
          .map(Java20Parser.IdentifierContext::getText)
          .collect(Collectors.joining("."));
      fileDataHandler.setPackageName(currentPackage);
    }
  }

  @Override
  public void enterSingleTypeImportDeclaration(
      final Java20Parser.SingleTypeImportDeclarationContext ctx) {
    // import typeName ;
    if (ctx.typeName() != null) {
      final String importName = getFullTypeName(ctx.typeName());
      fileDataHandler.addImport(importName);
    }
  }

  @Override
  public void enterTypeImportOnDemandDeclaration(
      final Java20Parser.TypeImportOnDemandDeclarationContext ctx) {
    // import packageOrTypeName.* ;
    if (ctx.packageOrTypeName() != null) {
      final String importName = getPackageOrTypeName(ctx.packageOrTypeName()) + ".*";
      fileDataHandler.addImport(importName);
      
      if (wildcardImportCount == 0 && wildcardImport == null) {
        wildcardImport = getPackageOrTypeName(ctx.packageOrTypeName());
      }
      wildcardImportCount++;
    }
  }

  @Override
  public void enterNormalClassDeclaration(final Java20Parser.NormalClassDeclarationContext ctx) {
    final String className = ctx.typeIdentifier().getText();
    final String fqn = buildFqn(className);
    
    fileDataHandler.enterClass(fqn);
    
    // Determine if abstract
    final boolean isAbstract = hasModifier(ctx.classModifier(), "abstract");
    if (isAbstract) {
      fileDataHandler.getCurrentClassData().setIsAbstractClass();
    } else {
      fileDataHandler.getCurrentClassData().setIsClass();
    }
    
    // Add modifiers
    addModifiers(ctx.classModifier());
    
    // Add LOC
    fileDataHandler.getCurrentClassData().addMetric(LOC, String.valueOf(getLoc(ctx)));
    
    // Handle extends
    if (ctx.classExtends() != null && ctx.classExtends().classType() != null) {
      final String superClass = getClassType(ctx.classExtends().classType());
      fileDataHandler.getCurrentClassData().setSuperClass(superClass);
    }
    
    // Handle implements
    if (ctx.classImplements() != null && ctx.classImplements().interfaceTypeList() != null) {
      for (final Java20Parser.InterfaceTypeContext intfCtx : 
           ctx.classImplements().interfaceTypeList().interfaceType()) {
        final String interfaceName = getClassType(intfCtx.classType());
        fileDataHandler.getCurrentClassData().addImplementedInterface(interfaceName);
      }
    }
  }

  @Override
  public void exitNormalClassDeclaration(final Java20Parser.NormalClassDeclarationContext ctx) {
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterNormalInterfaceDeclaration(
      final Java20Parser.NormalInterfaceDeclarationContext ctx) {
    final String interfaceName = ctx.typeIdentifier().getText();
    final String fqn = buildFqn(interfaceName);
    
    fileDataHandler.enterClass(fqn);
    fileDataHandler.getCurrentClassData().setIsInterface();
    
    // Add modifiers
    addModifiers(ctx.interfaceModifier());
    
    // Add LOC
    fileDataHandler.getCurrentClassData().addMetric(LOC, String.valueOf(getLoc(ctx)));
    
    // Handle extends
    if (ctx.interfaceExtends() != null && ctx.interfaceExtends().interfaceTypeList() != null) {
      for (final Java20Parser.InterfaceTypeContext intfCtx : 
           ctx.interfaceExtends().interfaceTypeList().interfaceType()) {
        final String parentInterface = getClassType(intfCtx.classType());
        fileDataHandler.getCurrentClassData().addImplementedInterface(parentInterface);
      }
    }
  }

  @Override
  public void exitNormalInterfaceDeclaration(
      final Java20Parser.NormalInterfaceDeclarationContext ctx) {
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterEnumDeclaration(final Java20Parser.EnumDeclarationContext ctx) {
    final String enumName = ctx.typeIdentifier().getText();
    final String fqn = buildFqn(enumName);
    
    fileDataHandler.enterClass(fqn);
    fileDataHandler.getCurrentClassData().setIsEnum();
    
    // Add modifiers
    addModifiers(ctx.classModifier());
    
    // Add LOC
    fileDataHandler.getCurrentClassData().addMetric(LOC, String.valueOf(getLoc(ctx)));
  }

  @Override
  public void exitEnumDeclaration(final Java20Parser.EnumDeclarationContext ctx) {
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterEnumConstant(final Java20Parser.EnumConstantContext ctx) {
    if (ctx.identifier() != null) {
      fileDataHandler.getCurrentClassData().addEnumConstant(ctx.identifier().getText());
    }
  }

  @Override
  public void enterFieldDeclaration(final Java20Parser.FieldDeclarationContext ctx) {
    if (ctx.variableDeclaratorList() != null && ctx.unannType() != null) {
      final String fieldType = resolveTypeName(ctx.unannType().getText());
      final List<String> modifiers = extractFieldModifiers(ctx);
      
      for (final Java20Parser.VariableDeclaratorContext varCtx : 
           ctx.variableDeclaratorList().variableDeclarator()) {
        final String fieldName = varCtx.variableDeclaratorId().identifier().getText();
        final String fieldFqn = fileDataHandler.getCurrentClassFqn() + "." + fieldName;
        fileDataHandler.enterMethod(fieldFqn);
        fileDataHandler.getCurrentClassData().addField(fieldName, fieldType, modifiers);
        fileDataHandler.leaveMethod();
      }
    }
  }

  @Override
  public void enterMethodDeclaration(final Java20Parser.MethodDeclarationContext ctx) {
    if (ctx.methodHeader() == null) {
      return;
    }
    
    final Java20Parser.MethodHeaderContext header = ctx.methodHeader();
    final Java20Parser.MethodDeclaratorContext declarator = header.methodDeclarator();
    
    if (declarator == null || declarator.identifier() == null) {
      return;
    }
    
    final String methodName = declarator.identifier().getText();
    final List<String> parameterTypes = extractParameterTypes(declarator);
    final String parameterHash = Verification.parameterHash(parameterTypes);
    final String methodFqn = fileDataHandler.getCurrentClassFqn() + "." + methodName 
        + "#" + parameterHash;
    
    fileDataHandler.enterMethod(methodFqn);
    
    // Get return type
    String returnType = "void";
    if (header.result() != null) {
      if (header.result().unannType() != null) {
        returnType = resolveTypeName(header.result().unannType().getText());
      }
    }
    
    final MethodDataHandler methodData = fileDataHandler.getCurrentClassData()
        .addMethod(methodFqn, returnType);
    
    // Add modifiers
    final List<String> modifiers = extractMethodModifiers(ctx);
    for (final String modifier : modifiers) {
      methodData.addModifier(modifier);
    }
    
    // Add parameters
    addParameters(methodData, declarator);
    
    // Add LOC
    methodData.addMetric(LOC, String.valueOf(getLoc(ctx)));
  }

  @Override
  public void exitMethodDeclaration(final Java20Parser.MethodDeclarationContext ctx) {
    fileDataHandler.leaveMethod();
  }

  @Override
  public void enterInterfaceMethodDeclaration(
      final Java20Parser.InterfaceMethodDeclarationContext ctx) {
    if (ctx.methodHeader() == null) {
      return;
    }
    
    final Java20Parser.MethodHeaderContext header = ctx.methodHeader();
    final Java20Parser.MethodDeclaratorContext declarator = header.methodDeclarator();
    
    if (declarator == null || declarator.identifier() == null) {
      return;
    }
    
    final String methodName = declarator.identifier().getText();
    final List<String> parameterTypes = extractParameterTypes(declarator);
    final String parameterHash = Verification.parameterHash(parameterTypes);
    final String methodFqn = fileDataHandler.getCurrentClassFqn() + "." + methodName 
        + "#" + parameterHash;
    
    fileDataHandler.enterMethod(methodFqn);
    
    // Get return type
    String returnType = "void";
    if (header.result() != null) {
      if (header.result().unannType() != null) {
        returnType = resolveTypeName(header.result().unannType().getText());
      }
    }
    
    final MethodDataHandler methodData = fileDataHandler.getCurrentClassData()
        .addMethod(methodFqn, returnType);
    
    // Add modifiers
    final List<String> modifiers = extractInterfaceMethodModifiers(ctx);
    for (final String modifier : modifiers) {
      methodData.addModifier(modifier);
    }
    
    // Add parameters
    addParameters(methodData, declarator);
    
    // Add LOC
    methodData.addMetric(LOC, String.valueOf(getLoc(ctx)));
  }

  @Override
  public void exitInterfaceMethodDeclaration(
      final Java20Parser.InterfaceMethodDeclarationContext ctx) {
    fileDataHandler.leaveMethod();
  }

  @Override
  public void enterConstructorDeclaration(final Java20Parser.ConstructorDeclarationContext ctx) {
    if (ctx.constructorDeclarator() == null 
        || ctx.constructorDeclarator().simpleTypeName() == null) {
      return;
    }
    
    final String constructorName = ctx.constructorDeclarator().simpleTypeName().getText();
    final List<String> parameterTypes = extractConstructorParameterTypes(
        ctx.constructorDeclarator());
    final String parameterHash = Verification.parameterHash(parameterTypes);
    final String constructorFqn = fileDataHandler.getCurrentClassFqn() + "." + constructorName 
        + "#" + parameterHash;
    
    fileDataHandler.enterMethod(constructorFqn);
    
    final MethodDataHandler constructor = fileDataHandler.getCurrentClassData()
        .addConstructor(constructorFqn);
    
    // Add modifiers
    final List<String> modifiers = extractConstructorModifiers(ctx);
    for (final String modifier : modifiers) {
      constructor.addModifier(modifier);
    }
    
    // Add parameters
    addConstructorParameters(constructor, ctx.constructorDeclarator());
    
    // Add LOC
    constructor.addMetric(LOC, String.valueOf(getLoc(ctx)));
  }

  @Override
  public void exitConstructorDeclaration(final Java20Parser.ConstructorDeclarationContext ctx) {
    fileDataHandler.leaveMethod();
  }

  // Helper methods

  private String buildFqn(final String className) {
    if (currentPackage.isEmpty()) {
      return className;
    }
    
    // Check if we're in a nested class
    try {
      final String currentFqn = fileDataHandler.getCurrentClassFqn();
      return currentFqn + "." + className;
    } catch (IllegalStateException e) {
      // Not in a class yet, use package
      return currentPackage + "." + className;
    }
  }

  private String getFullTypeName(final Java20Parser.TypeNameContext ctx) {
    // typeName: packageName ('.' typeIdentifier)?
    if (ctx.packageName() != null) {
      final String pkgName = getPackageName(ctx.packageName());
      if (ctx.typeIdentifier() != null) {
        return pkgName + "." + ctx.typeIdentifier().getText();
      }
      return pkgName;
    }
    return ctx.typeIdentifier() != null ? ctx.typeIdentifier().getText() : "";
  }
  
  private String getPackageName(final Java20Parser.PackageNameContext ctx) {
    // packageName: identifier ('.' packageName)?
    if (ctx.identifier() == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder(ctx.identifier().getText());
    if (ctx.packageName() != null) {
      sb.append(".").append(getPackageName(ctx.packageName()));
    }
    return sb.toString();
  }

  private String getPackageOrTypeName(final Java20Parser.PackageOrTypeNameContext ctx) {
    // Recursively build package/type name
    final List<Java20Parser.IdentifierContext> identifiers = new ArrayList<>();
    collectIdentifiers(ctx, identifiers);
    return identifiers.stream()
        .map(Java20Parser.IdentifierContext::getText)
        .collect(Collectors.joining("."));
  }

  private void collectIdentifiers(final Java20Parser.PackageOrTypeNameContext ctx,
      final List<Java20Parser.IdentifierContext> identifiers) {
    if (ctx.packageOrTypeName() != null) {
      collectIdentifiers(ctx.packageOrTypeName(), identifiers);
    }
    if (ctx.identifier() != null) {
      identifiers.add(ctx.identifier());
    }
  }

  private String getClassType(final Java20Parser.ClassTypeContext ctx) {
    // Simplified - just get the text
    return resolveTypeName(ctx.getText());
  }

  private boolean hasModifier(final List<? extends ParserRuleContext> modifiers,
      final String modifier) {
    if (modifiers == null) {
      return false;
    }
    for (final ParserRuleContext modCtx : modifiers) {
      if (modCtx.getText().equals(modifier)) {
        return true;
      }
    }
    return false;
  }

  private void addModifiers(final List<? extends ParserRuleContext> modifiers) {
    if (modifiers == null) {
      return;
    }
    for (final ParserRuleContext modCtx : modifiers) {
      final String modText = modCtx.getText();
      // Skip annotations (start with @)
      if (!modText.startsWith("@")) {
        fileDataHandler.getCurrentClassData().addModifier(modText);
      }
    }
  }

  private List<String> extractFieldModifiers(final Java20Parser.FieldDeclarationContext ctx) {
    // Field modifiers are part of the fieldDeclaration itself
    final List<String> modifiers = new ArrayList<>();
    
    if (ctx.fieldModifier() != null) {
      for (final Java20Parser.FieldModifierContext modCtx : ctx.fieldModifier()) {
        final String modText = modCtx.getText();
        if (!modText.startsWith("@")) {
          modifiers.add(modText);
        }
      }
    }
    
    return modifiers;
  }

  private List<String> extractMethodModifiers(final Java20Parser.MethodDeclarationContext ctx) {
    final List<String> modifiers = new ArrayList<>();
    if (ctx.methodModifier() != null) {
      for (final Java20Parser.MethodModifierContext modCtx : ctx.methodModifier()) {
        final String modText = modCtx.getText();
        if (!modText.startsWith("@")) {
          modifiers.add(modText);
        }
      }
    }
    return modifiers;
  }

  private List<String> extractConstructorModifiers(
      final Java20Parser.ConstructorDeclarationContext ctx) {
    final List<String> modifiers = new ArrayList<>();
    if (ctx.constructorModifier() != null) {
      for (final Java20Parser.ConstructorModifierContext modCtx : ctx.constructorModifier()) {
        final String modText = modCtx.getText();
        if (!modText.startsWith("@")) {
          modifiers.add(modText);
        }
      }
    }
    return modifiers;
  }

  private List<String> extractInterfaceMethodModifiers(
      final Java20Parser.InterfaceMethodDeclarationContext ctx) {
    final List<String> modifiers = new ArrayList<>();
    if (ctx.interfaceMethodModifier() != null) {
      for (final Java20Parser.InterfaceMethodModifierContext modCtx : 
           ctx.interfaceMethodModifier()) {
        final String modText = modCtx.getText();
        if (!modText.startsWith("@")) {
          modifiers.add(modText);
        }
      }
    }
    return modifiers;
  }

  private List<String> extractParameterTypes(final Java20Parser.MethodDeclaratorContext ctx) {
    final List<String> parameterTypes = new ArrayList<>();
    
    if (ctx.receiverParameter() != null) {
      // Skip receiver parameter (this)
    }
    
    if (ctx.formalParameterList() != null) {
      for (final Java20Parser.FormalParameterContext paramCtx : 
           ctx.formalParameterList().formalParameter()) {
        parameterTypes.add(paramCtx.unannType().getText());
      }
    }
    
    return parameterTypes;
  }

  private List<String> extractConstructorParameterTypes(
      final Java20Parser.ConstructorDeclaratorContext ctx) {
    final List<String> parameterTypes = new ArrayList<>();
    
    if (ctx.receiverParameter() != null) {
      // Skip receiver parameter (this)
    }
    
    if (ctx.formalParameterList() != null) {
      for (final Java20Parser.FormalParameterContext paramCtx : 
           ctx.formalParameterList().formalParameter()) {
        parameterTypes.add(paramCtx.unannType().getText());
      }
    }
    
    return parameterTypes;
  }

  private void addParameters(final MethodDataHandler methodData,
      final Java20Parser.MethodDeclaratorContext ctx) {
    if (ctx.formalParameterList() != null) {
      for (final Java20Parser.FormalParameterContext paramCtx : 
           ctx.formalParameterList().formalParameter()) {
        final String paramName = paramCtx.variableDeclaratorId().identifier().getText();
        final String paramType = resolveTypeName(paramCtx.unannType().getText());
        final List<String> modifiers = extractParameterModifiers(paramCtx);
        methodData.addParameter(paramName, paramType, modifiers);
      }
    }
  }

  private void addConstructorParameters(final MethodDataHandler methodData,
      final Java20Parser.ConstructorDeclaratorContext ctx) {
    if (ctx.formalParameterList() != null) {
      for (final Java20Parser.FormalParameterContext paramCtx : 
           ctx.formalParameterList().formalParameter()) {
        final String paramName = paramCtx.variableDeclaratorId().identifier().getText();
        final String paramType = resolveTypeName(paramCtx.unannType().getText());
        final List<String> modifiers = extractParameterModifiers(paramCtx);
        methodData.addParameter(paramName, paramType, modifiers);
      }
    }
  }

  private List<String> extractParameterModifiers(final Java20Parser.FormalParameterContext ctx) {
    final List<String> modifiers = new ArrayList<>();
    if (ctx.variableModifier() != null) {
      for (final Java20Parser.VariableModifierContext modCtx : ctx.variableModifier()) {
        final String modText = modCtx.getText();
        if (!modText.startsWith("@")) {
          modifiers.add(modText);
        }
      }
    }
    return modifiers;
  }

  private String resolveTypeName(final String typeName) {
    // Try to resolve the type using imports
    final List<String> imports = fileDataHandler.getImportNames();
    
    // Handle array types
    String baseType = typeName.replaceAll("\\[\\]", "");
    final String arraySuffix = typeName.substring(baseType.length());
    
    // Handle generics - extract the base type
    final int genericStart = baseType.indexOf('<');
    String genericsPart = "";
    if (genericStart != -1) {
      genericsPart = baseType.substring(genericStart);
      baseType = baseType.substring(0, genericStart);
    }
    
    // Check if it's a primitive type
    if (isPrimitiveType(baseType)) {
      return typeName;
    }
    
    // Check if already fully qualified
    if (baseType.contains(".")) {
      return typeName;
    }
    
    // Try to find in imports
    for (final String importName : imports) {
      if (importName.endsWith("." + baseType)) {
        return importName + genericsPart + arraySuffix;
      }
    }
    
    // Try wildcard import if enabled
    if (wildcardImportProperty && wildcardImportCount == 1 && wildcardImport != null) {
      return wildcardImport + "." + baseType + genericsPart + arraySuffix;
    }
    
    // Check java.lang package
    final List<String> javaLangTypes = Arrays.asList(
        "String", "Integer", "Long", "Double", "Float", "Boolean", "Character",
        "Byte", "Short", "Object", "Class", "System", "Math", "Thread",
        "Runnable", "Exception", "RuntimeException", "Error"
    );
    if (javaLangTypes.contains(baseType)) {
      return "java.lang." + baseType + genericsPart + arraySuffix;
    }
    
    // If in same package
    if (!currentPackage.isEmpty()) {
      return currentPackage + "." + typeName;
    }
    
    return typeName;
  }

  private boolean isPrimitiveType(final String type) {
    return Arrays.asList("byte", "short", "int", "long", "float", "double", 
        "boolean", "char", "void").contains(type);
  }

  private int getLoc(final ParserRuleContext ctx) {
    if (ctx.getStart() != null && ctx.getStop() != null) {
      final int startLine = ctx.getStart().getLine();
      final int endLine = ctx.getStop().getLine();
      return endLine - startLine + 1;
    }
    return 0;
  }

  private int getCloc(final ParserRuleContext ctx) {
    // Simplified comment counting
    // In a full implementation, you'd need to access the token stream
    return 0; // TODO: Implement proper comment counting
  }
}

