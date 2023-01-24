package net.explorviz.code.analysis.solver;

import com.github.javaparser.ast.type.Type;
import javax.annotation.Nonnull;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;

public interface TypeSolver {


  String solveType(@Nonnull String typeName) throws UnsolvedTypeException;

  String solveType(@Nonnull Type type) throws UnsolvedTypeException;


}
