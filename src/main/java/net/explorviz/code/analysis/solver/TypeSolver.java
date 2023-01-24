package net.explorviz.code.analysis.solver;

import com.github.javaparser.ast.type.Type;
import java.util.List;
import javax.annotation.Nonnull;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;

public interface TypeSolver {


  String solveType(@Nonnull String typeName) throws UnsolvedTypeException;

  String solveType(@Nonnull Type type) throws UnsolvedTypeException;

  void addImport(String importString);

  void addImports(List<String> importList);

  void setCurrentClassPath(String classPath);

  void reset();

}
