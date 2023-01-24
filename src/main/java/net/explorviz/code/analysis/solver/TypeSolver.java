package net.explorviz.code.analysis.solver;

import javax.annotation.Nonnull;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;

public interface TypeSolver {


  String solveType(@Nonnull String typeName) throws UnsolvedTypeException;


}
