package net.explorviz.code.analysis.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.explorviz.code.analysis.handler.MetricAppender;


public class ClassComplexityVisitor extends VoidVisitorAdapter<Pair<MetricAppender, Object>> {
  private static final String CYCLOMATIC_COMPLEXITY = "cyclomatic_complexity";
  private static final String CYCLOMATIC_COMPLEXITY_WEIGHTED = "cyclomatic_complexity_weighted";

  private final HashMap<String, Integer> methodCounter;

  public ClassComplexityVisitor() {
    methodCounter = new HashMap<>();
  }

  @Override
  public void visit(final ClassOrInterfaceDeclaration n, final Pair<MetricAppender, Object> data) {
    methodCounter.clear();

    data.a.enterClass(n);
    super.visit(n, data);
    int metricValue = 0;
    for (Map.Entry<String, Integer> entry : methodCounter.entrySet()) {
      metricValue += methodCounter.get(entry.getKey());
    }
    // set the class metric
    data.a.putClassMetric(CYCLOMATIC_COMPLEXITY, String.valueOf(metricValue));

    if (methodCounter.entrySet().size() > 0) {
      metricValue = metricValue / methodCounter.entrySet().size();
    }
    data.a.putClassMetric(CYCLOMATIC_COMPLEXITY_WEIGHTED, String.valueOf(metricValue));

    // Update the file metric
    Integer oldValue = Integer.getInteger(
        data.a.getFileData().getMetricValue(CYCLOMATIC_COMPLEXITY));
    if (oldValue == null) {
      data.a.putFileMetric(CYCLOMATIC_COMPLEXITY, String.valueOf(metricValue));
    } else {
      data.a.putFileMetric(CYCLOMATIC_COMPLEXITY, String.valueOf(oldValue + metricValue));
    }
    data.a.leaveClass();
  }


  @Override
  public void visit(final MethodDeclaration n, final Pair<MetricAppender, Object> data) {
    data.a.enterMethod(n);
    super.visit(n, data);
    int metricValue = methodCounter.getOrDefault(data.a.getCurrentMethodName(), 1);

    data.a.putMethodMetric(CYCLOMATIC_COMPLEXITY, String.valueOf(metricValue));
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


  @Override
  public void visit(ForEachStmt statement, final Pair<MetricAppender, Object> data) {
    addOccurrence(data.a.getCurrentMethodName(), "FOREACH");

    super.visit(statement, data);
  }


  @Override
  public void visit(ForStmt statement, final Pair<MetricAppender, Object> data) {
    addOccurrence(data.a.getCurrentMethodName(), "FOR");

    if (statement.getCompare().isPresent()) {
      conditionCheck(statement.getCompare().get().toString(), data);
    }

    super.visit(statement, data);
  }


  @Override
  public void visit(IfStmt statement, final Pair<MetricAppender, Object> data) {
    addOccurrence(data.a.getCurrentMethodName(), "IF");

    if (statement.getElseStmt().isPresent()) {
      addOccurrence(data.a.getCurrentMethodName(), "ELSE");
    }

    conditionCheck(statement.getCondition().toString(), data);

    super.visit(statement, data);
  }

  @Override
  public void visit(SwitchEntry statement, final Pair<MetricAppender, Object> data) {

    for (Statement st : statement.getStatements()) {
      addOccurrence(data.a.getCurrentMethodName(), "SWITCH");

    }

    super.visit(statement, data);
  }

  @Override
  public void visit(CatchClause statement, final Pair<MetricAppender, Object> data) {

    addOccurrence(data.a.getCurrentMethodName(), "CATCH");

    super.visit(statement, data);
  }

  @Override
  public void visit(ThrowStmt statement, final Pair<MetricAppender, Object> data) {

    addOccurrence(data.a.getCurrentMethodName(), "THROW");

    super.visit(statement, data);
  }


  @Override
  public void visit(TryStmt statement, final Pair<MetricAppender, Object> data) {

    addOccurrence(data.a.getCurrentMethodName(), "TRY");

    super.visit(statement, data);
  }


  @Override
  public void visit(WhileStmt statement, final Pair<MetricAppender, Object> data) {

    addOccurrence(data.a.getCurrentMethodName(), "WHILE");

    conditionCheck(statement.getCondition().toString(), data);

    super.visit(statement, data);
  }

  private void conditionCheck(String condition, final Pair<MetricAppender, Object> data) {
    regexCheck(condition, Pattern.compile("/(\\s|\\w|\\d)&(\\s|\\w|\\d)/xg"),
        "BITWISE_AND_OPERATOR", data);
    regexCheck(condition, Pattern.compile("/(\\s|\\w|\\d)\\|(\\s|\\w|\\d)/xg"),
        "BITWISE_OR_OPERATOR", data);
    regexCheck(condition, Pattern.compile("/(\\s|\\w|\\d)&&(\\s|\\w|\\d)/xg"), "AND_OPERATOR",
        data);
    regexCheck(condition, Pattern.compile("/(\\s|\\w|\\d)\\|\\|(\\s|\\w|\\d)/xg"), "OR_OPERATOR",
        data);
  }


  private void regexCheck(String haystack, Pattern pattern, String type,
                          final Pair<MetricAppender, Object> data) {
    Matcher matcher = pattern.matcher(haystack);

    while (matcher.find()) {
      addOccurrence(data.a.getCurrentMethodName(), type);
    }
  }

  private void addOccurrence(String methodName, String type) {

    // check if such method was ever updated
    if (methodCounter.containsKey(methodName)) {

      methodCounter.put(methodName, methodCounter.get(methodName) + 1);
    } else {

      // method name was never queried. create a new HashMap
      methodCounter.put(methodName, 1);
    }
  }

}