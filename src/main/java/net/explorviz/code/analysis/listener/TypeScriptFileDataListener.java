package net.explorviz.code.analysis.listener;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.code.analysis.antlr.generated.TypeScriptParser;
import net.explorviz.code.analysis.antlr.generated.TypeScriptParserBaseListener;
import net.explorviz.code.analysis.handler.TypeScriptFileDataHandler;
import net.explorviz.code.proto.FunctionData;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ANTLR Listener for extracting file data from TypeScript/JavaScript source code.
 */
public class TypeScriptFileDataListener extends TypeScriptParserBaseListener {

  public static final String FILE_SIZE = "size";
  public static final String LOC = "loc";
  public static final String CLOC = "cloc";

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptFileDataListener.class);

  private final TypeScriptFileDataHandler fileDataHandler;
  private final String fileExtension;

  public TypeScriptFileDataListener(final TypeScriptFileDataHandler fileDataHandler,
      final String fileExtension) {
    this.fileDataHandler = fileDataHandler;
    this.fileExtension = fileExtension;
  }

  @Override
  public void enterProgram(final TypeScriptParser.ProgramContext ctx) {
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
  public void enterImportStatement(final TypeScriptParser.ImportStatementContext ctx) {
    // Extract import statements
    // Example: import { foo } from 'bar';
    if (ctx.getText() != null) {
      final String importText = ctx.getText();
      fileDataHandler.addImport(importText);
      LOGGER.atTrace()
          .addArgument(importText)
          .log("Import: {}");
    }
  }

  @Override
  public void enterClassDeclaration(final TypeScriptParser.ClassDeclarationContext ctx) {
    // Extract class name
    if (ctx.identifier() != null) {
      final String className = ctx.identifier().getText();
      final String fqn = className; // TODO: Build proper FQN with module/namespace
      
      fileDataHandler.enterClass(fqn);
      
      LOGGER.atTrace()
          .addArgument(className)
          .log("Class: {}");
      
      // Calculate class LOC
      final int classLoc = calculateLoc(ctx);
      final var classData = fileDataHandler.getCurrentClassData();
      if (classData != null) {
        classData.addMetric(LOC, String.valueOf(classLoc));
      }
    }
  }

  @Override
  public void exitClassDeclaration(final TypeScriptParser.ClassDeclarationContext ctx) {
    // Leave class
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterInterfaceDeclaration(final TypeScriptParser.InterfaceDeclarationContext ctx) {
    // Extract interface name
    if (ctx.identifier() != null) {
      final String interfaceName = ctx.identifier().getText();
      final String fqn = interfaceName; // TODO: Build proper FQN
      
      fileDataHandler.enterClass(fqn);
      final var classData = fileDataHandler.getCurrentClassData();
      if (classData != null) {
        classData.setIsInterface();
        
        // Calculate interface LOC
        final int interfaceLoc = calculateLoc(ctx);
        classData.addMetric(LOC, String.valueOf(interfaceLoc));
      }
      
      LOGGER.atTrace()
          .addArgument(interfaceName)
          .log("Interface: {}");
    }
  }

  @Override
  public void exitInterfaceDeclaration(final TypeScriptParser.InterfaceDeclarationContext ctx) {
    // Leave interface
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterFunctionDeclaration(final TypeScriptParser.FunctionDeclarationContext ctx) {
    // Extract function name
    if (ctx.identifier() != null) {
      final String functionName = ctx.identifier().getText();
      
      // Check if we're inside a class or this is a global function
      if (fileDataHandler.isInClassContext()) {
        // Function inside a class - treat as a method
        final String functionFqn = functionName + "#1"; // TODO: Add proper parameter hashing
        
        final var methodData = fileDataHandler.getCurrentClassData()
            .addMethod(functionFqn, "void"); // TODO: Extract actual return type
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Function inside class: {}");
        
        // Calculate function LOC
        final int functionLoc = calculateLoc(ctx);
        methodData.addMetric(LOC, String.valueOf(functionLoc));
      } else {
        // Global function - track it separately!
        final FunctionData.Builder funcBuilder = fileDataHandler.addGlobalFunction(
            functionName,
            "void"  // TODO: Extract actual return type
        );
        
        // Set function location
        if (ctx.start != null && ctx.stop != null) {
          funcBuilder.setStartLine(ctx.start.getLine());
          funcBuilder.setEndLine(ctx.stop.getLine());
        }
        
        // Calculate LOC
        final int functionLoc = calculateLoc(ctx);
        funcBuilder.putMetric(LOC, String.valueOf(functionLoc));
        
        // Check for async
        // TODO: Detect async functions
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Global function: {}");
      }
    }
  }

  @Override
  public void exitFunctionDeclaration(final TypeScriptParser.FunctionDeclarationContext ctx) {
    // Nothing special to do on exit for now
  }

  @Override
  public void enterArrowFunctionDeclaration(
      final TypeScriptParser.ArrowFunctionDeclarationContext ctx) {
    // Handle arrow functions: const foo = () => {}
    // Arrow functions are typically assigned to variables, so we need to extract name
    
    // For now, we'll try to get the identifier from the parent context (variable declaration)
    String functionName = extractArrowFunctionName(ctx);
    
    if (functionName != null) {
      if (fileDataHandler.isInClassContext()) {
        // Arrow function inside a class (e.g., class field)
        final String functionFqn = functionName + "#1";
        
        final var methodData = fileDataHandler.getCurrentClassData()
            .addMethod(functionFqn, "void");
        
        // Calculate method LOC
        final int methodLoc = calculateLoc(ctx);
        methodData.addMetric(LOC, String.valueOf(methodLoc));
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Arrow function inside class: {}");
      } else {
        // Global arrow function
        final FunctionData.Builder funcBuilder = fileDataHandler.addGlobalFunction(
            functionName,
            "void"
        );
        
        // Set function location
        if (ctx.start != null && ctx.stop != null) {
          funcBuilder.setStartLine(ctx.start.getLine());
          funcBuilder.setEndLine(ctx.stop.getLine());
        }
        
        // Calculate LOC
        final int functionLoc = calculateLoc(ctx);
        funcBuilder.putMetric(LOC, String.valueOf(functionLoc));
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Global arrow function: {}");
      }
    } else {
      // Anonymous arrow function - count it but don't add as named function
      LOGGER.atTrace().log("Anonymous arrow function detected");
    }
  }
  
  /**
   * Extract the name of an arrow function from its parent context.
   * Arrow functions are often assigned to variables: const foo = () => {}
   *
   * <p>For now, we use a simple heuristic: try to extract text from nearby identifiers
   */
  private String extractArrowFunctionName(
      final TypeScriptParser.ArrowFunctionDeclarationContext ctx) {
    // Simple approach: look for identifiers in parent contexts
    ParserRuleContext parent = ctx.getParent();
    
    int depth = 0;
    while (parent != null && depth < 5) {
      // Try to find any identifier in the parent context
      String text = parent.getText();
      if (text != null && text.contains("=")) {
        // Likely a variable assignment: const foo = () => {}
        String[] parts = text.split("=");
        if (parts.length > 0) {
          String potentialName = parts[0].trim();
          // Remove keywords like const, let, var
          potentialName = potentialName.replaceAll("^(const|let|var)\\s+", "");
          if (potentialName.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            return potentialName;
          }
        }
      }
      parent = parent.getParent();
      depth++;
    }
    
    return null; // Anonymous arrow function
  }

  /**
   * Calculate lines of code for a given context.
   */
  private int calculateLoc(final ParserRuleContext ctx) {
    if (ctx == null || ctx.start == null || ctx.stop == null) {
      return 0;
    }
    return ctx.stop.getLine() - ctx.start.getLine() + 1;
  }

  /**
   * Get total lines of code.
   */
  private int getLoc(final ParserRuleContext ctx) {
    if (ctx == null || ctx.stop == null) {
      return 0;
    }
    return ctx.stop.getLine();
  }

  /**
   * Get comment lines of code.
   * TODO: Implement proper comment counting for JS/TS
   */
  private int getCloc(final ParserRuleContext ctx) {
    // For now, return 0. We'll implement this later by counting comment tokens.
    return 0;
  }
}

