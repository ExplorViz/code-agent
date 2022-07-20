package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.List;

public class InheritanceVisitor extends VoidVisitorAdapter<List<String>> {
  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final List<String> collector) {
    super.visit(n, collector);

    for (final ClassOrInterfaceType c : n.getExtendedTypes()) {
      try {
        collector.add(c.resolve().describe());
      } catch (final UnsolvedSymbolException e) {
        collector.add("Unresolvable super class: " + c.getNameAsString());
      }
    }
  }
}

