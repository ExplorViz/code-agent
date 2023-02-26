package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import net.explorviz.code.analysis.handler.MetricAppender;

public class NestedBlockDepth extends VoidVisitorAdapter<Pair<MetricAppender, Object>> {

  private int maxDepth = 0;
  private int currentDepth = 0;

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Pair<MetricAppender, Object> data) {
    data.a.enterClass(n);
    super.visit(n, data);
    data.a.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final Pair<MetricAppender, Object> data) {
    data.a.enterMethod(n);
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, data);
    data.a.putMethodMetric("nestedBlockDepth", String.valueOf(maxDepth));
    data.a.leaveMethod();
    maxDepth = 0;
    currentDepth = 0;
  }

  @Override
  public void visit(ForStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(ForEachStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(WhileStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(DoStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(IfStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(TryStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(SwitchEntry n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(SwitchStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(LambdaExpr n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }

  @Override
  public void visit(SynchronizedStmt n, Pair<MetricAppender, Object> arg) {
    currentDepth++;
    maxDepth = Math.max(maxDepth, currentDepth);
    super.visit(n, arg);
    currentDepth--;
  }
}
