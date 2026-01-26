package net.explorviz.code.analysis.listener;

import net.explorviz.code.analysis.antlr.generated.PythonLexer;
import net.explorviz.code.analysis.antlr.generated.PythonParser;
import net.explorviz.code.analysis.antlr.generated.PythonParserBaseListener;
import net.explorviz.code.analysis.handler.PythonFileDataHandler;
import net.explorviz.code.proto.FunctionData;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
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
  private final CommonTokenStream tokens;

  public PythonFileDataListener(final PythonFileDataHandler fileDataHandler,
      final CommonTokenStream tokens) {
    this.fileDataHandler = fileDataHandler;
    this.tokens = tokens;
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
    if (ctx.name() == null) {
      return;
    }

    // Extract function name
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
      final var funcBuilder = fileDataHandler.addGlobalFunction(
          functionName,
          "None" // TODO: Extract actual return type from type hints
      );

      // Set function location - find actual start/end lines
      int startLine = ctx.start != null ? ctx.start.getLine() : 0;
      int endLine = startLine;

      // Workaround for ANTLR Python grammar issue where ctx.stop includes next
      // function
      // See: https://github.com/antlr/grammars-v4/issues/4153
      if (ctx.suite() != null) {
        final var suite = ctx.suite();

        if (suite.simple_stmt() != null && suite.simple_stmt().stop != null) {
          endLine = suite.simple_stmt().stop.getLine();
        } else {
          // Multi-line function body - find the DEDENT token that marks end of function
          // Python uses INDENT/DEDENT tokens to mark indentation blocks
          if (suite.stop != null) {
            final int suiteStopIndex = suite.stop.getTokenIndex();

            for (int i = suiteStopIndex; i >= 0; i--) {
              final Token token = tokens.get(i);

              // Skip DEDENT, NEWLINE, LINE_BREAK, and COMMENT tokens
              if (token.getType() == PythonLexer.DEDENT
                  || token.getType() == PythonLexer.NEWLINE
                  || token.getType() == PythonLexer.LINE_BREAK
                  || token.getType() == PythonLexer.COMMENT) {
                continue;
              }

              // Found the last actual code token
              endLine = token.getLine();
              break;
            }
          }
        }
      }

      funcBuilder.setLines(startLine, endLine);

      // Calculate LOC using actual start and end lines
      final int functionLoc = (endLine >= startLine) ? (endLine - startLine + 1) : 0;
      funcBuilder.addMetric(LOC, String.valueOf(functionLoc));

      LOGGER.atTrace()
          .addArgument(functionName)
          .log("Global function: {}");
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
