package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import net.explorviz.code.analysis.handler.FileDataHandler;

/**
 * Visitor filling a FileData object with NPath data. Only handles constructor and methods bodies.
 * This implementation is a reimplementation of the {@code NPathBaseVisitor} from PMD.
 */
public class NPathVisitor extends GenericVisitorAdapter<Integer, FileDataHandler> {


  @Override
  public Integer visit(ConstructorDeclaration n, FileDataHandler data) {
    return super.visit(n, data);
  }

  @Override
  public Integer visit(MethodDeclaration n, FileDataHandler data) {
    // start calculating here
    // int a = super.visit(n, data);
    // System.out.println(a);
    int ret;
    if (n.getBody().isPresent()) {
      ret = visit(n.getBody().get(), data);
    } else {
      ret = 1;
    }
    System.out.println(ret);
    return ret;
  }

  @Override
  public Integer visit(BlockStmt n, FileDataHandler data) {
    int product = 1;
    for (Node node : n.getChildNodes()) {
      // Skip comment
      if (node instanceof Comment) {
        continue;
      }
      Integer childComplexity = node.accept(this, data);
      if (childComplexity != null) {
        product *= childComplexity;
      }

    }

    return product;
  }

  @Override
  public Integer visit(AssignExpr n, FileDataHandler arg) {
    return 1;
  }

  @Override
  public Integer visit(UnaryExpr n, FileDataHandler arg) {
    return 1;
  }

  @Override
  public Integer visit(MethodCallExpr n, FileDataHandler arg) {
    return super.visit(n, arg);
  }

  @Override
  public Integer visit(ExpressionStmt n, FileDataHandler arg) {
    return super.visit(n, arg);
  }

  @Override
  public Integer visit(IfStmt n, FileDataHandler data) {
    // (npath of if + npath of else (or 1) + bool_comp of if) * npath of next
    int complexity = n.hasElseBlock() ? 0 : 1;
    // filter for statements
    complexity += n.getThenStmt().accept(this, data);
    if (n.hasElseBlock() && n.getElseStmt().isPresent()) {
      complexity += n.getElseStmt().get().accept(this, data);
    }

    int boolCompIf = booleanExpressionComplexity(n.getCondition());
    return boolCompIf + complexity;
  }

  @Override
  public Integer visit(WhileStmt n, FileDataHandler data) {
    // (npath of while + bool_comp of while + 1) * npath of next
    int boolCompWhile = booleanExpressionComplexity(n.getCondition());

    int nPathWhile = n.getBody().accept(this, data);

    return boolCompWhile + nPathWhile + 1;
  }

  @Override
  public Integer visit(DoStmt n, FileDataHandler data) {
    // (npath of do + bool_comp of do + 1) * npath of next
    int boolCompDo = booleanExpressionComplexity(n.getCondition());

    int nPathDo = n.getBody().accept(this, data);

    return boolCompDo + nPathDo + 1;
  }

  @Override
  public Integer visit(ForStmt n, FileDataHandler data) {
    // (npath of for + bool_comp of for + 1) * npath of next
    int boolCompForInit = 0;
    for (Expression e : n.getInitialization()) {
      boolCompForInit += booleanExpressionComplexity(e);
    }
    int boolCompForCompare = 0;
    if (n.getCompare().isPresent()) {
      boolCompForCompare = booleanExpressionComplexity(n.getCompare().get());
    }
    int boolCompForUpdate = 0;
    for (Expression e : n.getUpdate()) {
      boolCompForUpdate += booleanExpressionComplexity(e);
    }
    int nPathFor = n.getBody().accept(this, data);

    return boolCompForInit + boolCompForCompare + boolCompForUpdate + nPathFor + 1;
  }

  @Override
  public Integer visit(ReturnStmt n, FileDataHandler data) {
    // return statements are valued at 1, or the value of the boolean expression
    if (n.getExpression().isEmpty()) {
      return 1;
    }
    int boolCompReturn = booleanExpressionComplexity(n.getExpression().get());
    // int conditionalExpressionComplexity = multiply(n.getExpression().get(), data);
    int conditionalExpressionComplexity = 0;

    if (conditionalExpressionComplexity > 1) {
      boolCompReturn += conditionalExpressionComplexity;
    }
    return boolCompReturn > 0 ? conditionalExpressionComplexity : 1;
  }

  // @Override
  // public Integer visit(BreakStmt n, FileDataHandler arg) {
  //   return 1;
  // }

  // @Override
  // public Integer visit(SwitchExpr n, FileDataHandler data) {
  //   // bool_comp of switch + sum(npath(case_range))
  //   return super.visit(n, data);
  // }

  @Override
  public Integer visit(SwitchStmt n, FileDataHandler data) {
    // bool_comp of switch + sum(npath(case_range))

    int boolCompSwitch = booleanExpressionComplexity(n.getSelector());

    //
    int npath = 0;
    //
    int caseRange = 0;
    for (SwitchEntry entry : n.getEntries()) {
      if (entry.getType().equals(SwitchEntry.Type.STATEMENT_GROUP) || entry.getType()
          .equals(SwitchEntry.Type.THROWS_STATEMENT)) {
        npath += caseRange;
        caseRange = 1;
      } else if (entry.getType().equals(SwitchEntry.Type.EXPRESSION) || entry.getType().equals(
          SwitchEntry.Type.BLOCK)) {
        npath += caseRange;
        int complexity = entry.accept(this, data);
        caseRange = complexity;
      } else {
        int complexity = entry.accept(this, data);
        caseRange *= complexity;
      }
    }
    // add in npath of last label
    npath += caseRange;
    return boolCompSwitch + npath;
  }

  @Override
  public Integer visit(SwitchEntry n, FileDataHandler data) {
    // this is the <default> case
    if (n.getLabels().isEmpty()) {
      return 1;
    }
    return n.getLabels().size();
  }

  // @Override
  // public Integer visit(BinaryExpr n, FileDataHandler data) {
  //   if (n.getOperator().equals(BinaryExpr.Operator.AND) || n.getOperator()
  //       .equals(BinaryExpr.Operator.OR)) {
  //     Integer i = super.visit(n, data);
  //     return i == null ? 1 : i + 1;
  //   }
  //   Integer i = super.visit(n, data);
  //   return i == null ? 0 : i;
  // }

  @Override
  public Integer visit(ConditionalExpr n, FileDataHandler data) {
    // bool comp of guard clause + complexity of last two children (= total - 1)
    int boolCompTernary = booleanExpressionComplexity(n.getCondition());
    int thenValue = n.getThenExpr().accept(this, data);
    int elseValue = n.getElseExpr().accept(this, data);
    return boolCompTernary + thenValue + elseValue + 2;
  }

  @Override
  public Integer visit(TryStmt n, FileDataHandler data) {
    /*
     * This scenario was not addressed by the original paper. Based on the
     * principles outlined in the paper, as well as the Checkstyle NPath
     * implementation, this code will add the complexity of the try to the
     * complexities of the catch and finally blocks.
     */
    int sum = 0;
    for (Node node : n.getChildNodes()) {
      sum += node.accept(this, data);
    }
    return sum;
  }

  private int booleanExpressionComplexity(Expression expr) {
    if (expr == null) {
      return 0;
    }

    if (expr.isBinaryExpr()) {
      boolean isAndOperator = expr.asBinaryExpr().getOperator()
          .equals(BinaryExpr.Operator.AND);
      boolean isOrOperator = expr.asBinaryExpr().getOperator()
          .equals(BinaryExpr.Operator.OR);
      if (isAndOperator || isOrOperator) {
        int complexity = 1;
        complexity += booleanExpressionComplexity(expr.asBinaryExpr().getLeft());
        complexity += booleanExpressionComplexity(expr.asBinaryExpr().getRight());
        return complexity;
      } else {
        return 0;
      }

    } else {
      return 0;
    }
  }
}
