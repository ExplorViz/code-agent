package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import net.explorviz.code.analysis.handler.MetricAppender;

/**
 * This is a starting point for the implementation of further metrics. Use this stub as basis for
 * your metrics collector. The type of the VoidVisitorAdapter is set to a Pair where Pair.a is
 * reserved for the MetricAppender. Part.b is free to use and can be changed to whatever type is
 * desired. The MetricAppender is a convenient way to add metrics to the internal File
 * representation without the need to construct the fqn and keep track of the current Class-Method
 * hierarchy. If more direct access is required, use the
 * {@link net.explorviz.code.analysis.handler.FileDataHandler} directly but you have to handle all
 * the fqn tracking yourself.
 */
public class VisitorStub extends VoidVisitorAdapter<Pair<MetricAppender, Object>> {

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Pair<MetricAppender, Object> data) {
    data.a.enterClass(n);
    data.a.putClassMetric("someClassMetric", "metricValue");
    super.visit(n, data);
    data.a.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final Pair<MetricAppender, Object> data) {
    data.a.enterMethod(n);
    data.a.putMethodMetric("someMethodMetric", "metricValue");
    super.visit(n, data);
    data.a.leaveMethod();
  }

  @Override
  public void visit(ObjectCreationExpr n, Pair<MetricAppender, Object> data) {
    if (n.getAnonymousClassBody().isPresent()) {
      if (n.getAnonymousClassBody().get().size() > 1) {
        // TODO did not found an example how this could look like, so an implementation is needed
      }
      data.a.enterAnonymousClass(n.getTypeAsString(), data.a.getCurrentMethodName());
      super.visit(n, data);
      data.a.leaveAnonymousClass();
    } else {
      super.visit(n, data);
    }
  }
}
