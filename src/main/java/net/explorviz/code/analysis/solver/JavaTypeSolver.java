package net.explorviz.code.analysis.solver;

import com.github.javaparser.ast.type.Type;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;
import static net.explorviz.code.analysis.types.JavaTypes.built_ins;
import static net.explorviz.code.analysis.types.JavaTypes.primitives;

public class JavaTypeSolver implements TypeSolver {

  private String path;

  public JavaTypeSolver(final String path) {
    this.path = path;
  }

  public JavaTypeSolver() {
  }

  @Override
  public String solveType(@Nonnull final String typeName) throws UnsolvedTypeException {
    Optional<String> fqn = checkPrimitive(typeName);
    if (fqn.isPresent()) {
      return fqn.get();
    }
    fqn = checkBuiltIn(typeName);
    if (fqn.isPresent()) {
      return fqn.get();
    }
    throw new UnsolvedTypeException(typeName);
  }

  @Override
  public String solveType(@Nonnull Type type) throws UnsolvedTypeException {
    throw new UnsolvedTypeException(type.asString());
  }

  private Optional<String> checkPrimitive(final String typeName) {
    for (final String primitive : primitives) {
      if (typeName.equals(primitive)) {
        return Optional.of(typeName);
      }
    }
    return Optional.empty();
  }

  private Optional<String> checkBuiltIn(final String typeName) {
    for (final String built_in : built_ins) {
      if (typeName.equals(built_in)) {
        return Optional.of("java.lang." + typeName);
      }
    }
    return Optional.empty();
  }

  private Optional<String> checkImports(final String typeName, final List<String> imports) {
    for (final String importEntry : imports) {
      if (importEntry.endsWith(typeName)) {
        return Optional.of(importEntry);
      }
    }
    return Optional.empty();
  }
}
