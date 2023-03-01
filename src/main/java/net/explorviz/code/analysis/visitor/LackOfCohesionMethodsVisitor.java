package net.explorviz.code.analysis.visitor;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.handler.MetricAppender;
import net.explorviz.code.analysis.types.Graph;

/**
 * Basic LCOM4 implementation.
 */
public class LackOfCohesionMethodsVisitor extends VoidVisitorAdapter<Pair<MetricAppender, Object>> {

  private Graph graph;
  private List<FieldDeclaration> fields;
  private List<String> fieldNames;
  private List<String> methodNames;

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Pair<MetricAppender, Object> data) {
    this.graph = new Graph();
    data.a.enterClass(n);

    fields = n.getFields();
    fieldNames = n.getFields().stream()
        .map(field -> field.getVariables().getFirst().get().getName().asString())
        .collect(Collectors.toList());
    for (final String fieldName : fieldNames) {
      graph.addVertex(fieldName, true);
    }
    methodNames = n.getMethods().stream().map(NodeWithSimpleName::getNameAsString)
        .collect(Collectors.toList());
    for (final String methodName : methodNames) {
      graph.addVertex(methodName);
    }

    super.visit(n, data);

    try {
      data.a.putClassMetric("LCOM4", String.valueOf(graph.getGroups().size()));
    } catch (NotFoundException e) {
      throw new RuntimeException(e); // NOPMD
    }
    data.a.leaveClass();
  }

  @Override // NOCS
  public void visit(final MethodDeclaration n, // NOCS NOPMD
                    final Pair<MetricAppender, Object> data) { // NOCS NOPMD
    data.a.enterMethod(n);
    // Skip this method if it is inherited, remove the graph entry
    if (n.isAnnotationPresent("Override")) {
      graph.removeVertex(n.getNameAsString());
      return;
    }
    // it the method is empty, remove the graph entry
    if (n.getBody().isEmpty() || n.getBody().get().getStatements().isEmpty() && n.getBody().get()
        .getChildNodes().isEmpty()) {
      graph.removeVertex(n.getNameAsString());
      return;
    }
    // field access
    for (final FieldAccessExpr expr : n.findAll(FieldAccessExpr.class)) {
      if (expr.isInternal()) {
        continue;
      }
      if (findInClassFields(expr.getNameAsString())) {
        graph.addEdge(n.getNameAsString(), expr.getNameAsString());
      }
    }
    final List<Pair<String, Optional<Range>>> localVariables = new ArrayList<>();
    for (final VariableDeclarator variableDeclarator : n.findAll(VariableDeclarator.class)) {
      localVariables.add(
          new Pair<>(variableDeclarator.getNameAsString(), variableDeclarator.getRange()));
    }

    for (final NameExpr nameExpr : n.findAll(NameExpr.class)) {
      if (fieldNames.contains(nameExpr.getNameAsString()) && isNotShadowedByLocalVariable(
          localVariables, nameExpr)) {
        graph.addEdge(n.getNameAsString(), nameExpr.getNameAsString());
      }
    }

    for (final MethodCallExpr method : n.findAll(MethodCallExpr.class)) {
      if (isClassMethod(method)) {
        // add something to method name so fields and methods can have the same name
        graph.addEdge(n.getNameAsString(), method.getNameAsString());
      }

    }
    super.visit(n, data);
    data.a.leaveMethod();
  }

  @Override
  public void visit(final ObjectCreationExpr n, final Pair<MetricAppender, Object> data) {
    if (n.getAnonymousClassBody().isPresent()) {
      for (final Node node : n.getChildNodes()) {
        if (node instanceof ClassOrInterfaceDeclaration) {
          data.a.enterAnonymousClass(n.getTypeAsString(), data.a.getCurrentMethodName());
          node.accept(this, data);
          data.a.leaveAnonymousClass();
        } else {
          node.accept(this, data);
        }
      }
    } else {
      super.visit(n, data);
    }
  }

  private boolean isNotShadowedByLocalVariable(
      final List<Pair<String, Optional<Range>>> localVariables, final NameExpr nameExpr) {
    for (final Pair<String, Optional<Range>> entry : localVariables) {
      if (nameExpr.getNameAsString().equals(entry.a) && entry.b.isPresent() && nameExpr.getRange()
          .isPresent()) {
        return entry.b.get().isAfter(nameExpr.getRange().get().end);

      }
    }
    return true;
  }

  private boolean findInClassFields(final String field) {
    for (final FieldDeclaration f : fields) {
      if (f.getVariables().getFirst().isPresent() && field.contains(
          f.getVariables().getFirst().get().getName().toString())) {
        return true;
      }
    }
    return false;
  }

  private boolean isClassMethod(final MethodCallExpr method) {
    if (method.getScope().isEmpty() || method.getScope().get().isThisExpr()) {
      return methodNames.contains(method.getNameAsString());
    } else {
      return false;
    }
  }
}