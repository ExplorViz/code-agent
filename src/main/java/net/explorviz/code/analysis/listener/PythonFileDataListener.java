package net.explorviz.code.analysis.listener;

import net.explorviz.code.analysis.antlr.generated.PythonParser;
import net.explorviz.code.analysis.antlr.generated.PythonParserBaseListener;
import net.explorviz.code.analysis.handler.PythonFileDataHandler;
import net.explorviz.code.proto.FunctionData;
import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ANTLR Listener for extracting file data from Python source code.
 */
public class PythonFileDataListener extends PythonParserBaseListener {

  public static final String FILE_SIZE = "size";
  public static final String LOC = "loc";
  public static final String CLOC = "cloc";

  private static final Logger LOGGER = LoggerFactory.getLogger(PythonFileDataListener.class);

  private final PythonFileDataHandler fileDataHandler;

  public PythonFileDataListener(final PythonFileDataHandler fileDataHandler) {
    this.fileDataHandler = fileDataHandler;
  }

  @Override
  public void enterFile_input(final PythonParser.File_inputContext ctx) {
    // Calculate LOC and CLOC
    final int loc = getLoc(ctx);
    final int cloc = 0; // TODO: Implement proper comment counting

    fileDataHandler.addMetric(LOC, String.valueOf(loc));
    fileDataHandler.addMetric(CLOC, String.valueOf(cloc));

    LOGGER.atTrace()
        .addArgument(fileDataHandler.getFileName())
        .addArgument(loc)
        .log("{} - LOC: {}");
  }

  @Override
  public void enterImport_stmt(final PythonParser.Import_stmtContext ctx) {
    // Extract import statements
    if (ctx.getText() != null) {
      final String importText = ctx.getText();
      fileDataHandler.addImport(importText);
      LOGGER.atTrace()
          .addArgument(importText)
          .log("Import: {}");
    }
  }

  @Override
  public void enterClassdef(final PythonParser.ClassdefContext ctx) {
    // Extract class name
    if (ctx.name() != null) {
      final String className = ctx.name().getText();
      
      fileDataHandler.enterClass(className);
      
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
  public void exitClassdef(final PythonParser.ClassdefContext ctx) {
    // Leave class
    fileDataHandler.leaveClass();
  }

  @Override
  public void enterFuncdef(final PythonParser.FuncdefContext ctx) {
    // Extract function name
    if (ctx.name() != null) {
      final String functionName = ctx.name().getText();
      
      // Check if we're inside a class or this is a global function
      if (fileDataHandler.isInClassContext()) {
        // Function inside a class - treat as a method
        final String functionFqn = functionName + "#1"; // TODO: Add proper parameter hashing
        
        final var methodData = fileDataHandler.getCurrentClassData()
            .addMethod(functionFqn, "None"); // Python default return is None
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Method: {}");
        
        // Calculate function LOC
        final int functionLoc = calculateLoc(ctx);
        methodData.addMetric(LOC, String.valueOf(functionLoc));
        
        // Check for async - commented out for now
        // TODO: Add async support to MethodDataHandler if needed
      } else {
        // Global function
        final FunctionData.Builder funcBuilder = fileDataHandler.addGlobalFunction(
            functionName,
            "None"  // TODO: Extract actual return type from type hints
        );
        
        // Set function location
        if (ctx.start != null && ctx.stop != null) {
          funcBuilder.setStartLine(ctx.start.getLine());
          funcBuilder.setEndLine(ctx.stop.getLine());
        }
        
        // Calculate LOC
        final int functionLoc = calculateLoc(ctx);
        funcBuilder.putMetric(LOC, String.valueOf(functionLoc));
        
        // Check for async - commented out for now as FunctionData.Builder may not support async
        // TODO: Add async support if needed
        // if (ctx.ASYNC() != null) {
        //   funcBuilder.setAsync(true);
        // }
        
        LOGGER.atTrace()
            .addArgument(functionName)
            .log("Global function: {}");
      }
    }
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
}
