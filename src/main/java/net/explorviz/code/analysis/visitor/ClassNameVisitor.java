package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.List;

/**
 * Collects class names.
 */
public class ClassNameVisitor extends VoidVisitorAdapter<List<String>> {
  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final List<String> collector) {
    super.visit(n, collector);
    collector.add(n.getFullyQualifiedName().orElse("UNKNOWN"));
  }

}
