package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.List;

public class MethocCallVisitor extends VoidVisitorAdapter<List<String>> {

  @Override
  public void visit(final MethodCallExpr n, final List<String> collector) {
    super.visit(n, collector);

    final String type = "UNKNOWN";

    /*
     * if (isPrimitiveType(n.getScope().get())) { type = n.asTypeExpr().getTypeAsString(); } else {
     * try { type = n.resolve().getQualifiedName(); } catch (UnsolvedSymbolException e) {
     *
     * } }
     */

    /*
     * if (n.getScope().isPresent()) { Expression expr = n.getScope().get();
     *
     * if (expr.isFieldAccessExpr()) { type =
     * expr.asFieldAccessExpr().resolve().getType().toString(); } else if (expr.isNameExpr()) { type
     * = expr.asNameExpr().resolve().getType().toString(); } }
     */

    // for (final Expression a : n.getArguments()) {
    // System.out.println(a);
    // }

    // if (n.getScope().isPresent()) {
    // System.out.println("ALEX: " + n.getScope().get().calculateResolvedType());
    // System.out.println("ALEX: " + n.getScope().get());
    // }

    collector.add(type + " - " + n.getScope() + " - " + n.getName());
  }

}
