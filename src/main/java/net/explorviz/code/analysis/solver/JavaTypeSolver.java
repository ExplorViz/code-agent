package net.explorviz.code.analysis.solver;

import com.github.javaparser.ast.type.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import net.explorviz.code.analysis.exceptions.UnsolvedTypeException;
import static net.explorviz.code.analysis.types.JavaTypes.built_ins;
import static net.explorviz.code.analysis.types.JavaTypes.primitives;

public class JavaTypeSolver implements TypeSolver {

  private String path;
  private final Set<String> imports;

  public JavaTypeSolver(final String path) {
    this.path = path;
    this.imports = new HashSet<>();
  }

  public JavaTypeSolver() {
    this.imports = new HashSet<>();
  }

  @Override
  public String solveType(@Nonnull String typeName) throws UnsolvedTypeException {
    int arrayDimension = 0;

    while (typeName.endsWith("[]")) {
      arrayDimension++;
      typeName = typeName.replaceFirst("\\[]", "");
    }
    System.out.println(typeName + "  " + arrayDimension);
    // TODO check if array
    // TODO check if generic
    Optional<String> fqn = checkPrimitive(typeName);
    if (fqn.isPresent()) {
      return fqn.get() + "[]".repeat(arrayDimension);
    }
    fqn = checkBuiltIn(typeName);
    if (fqn.isPresent()) {
      return fqn.get() + "[]".repeat(arrayDimension);
    }
    fqn = checkImports(typeName);
    if (fqn.isPresent()) {
      return fqn.get() + "[]".repeat(arrayDimension);
    }
    throw new UnsolvedTypeException(typeName);
  }

  @Override
  public String solveType(@Nonnull Type type) throws UnsolvedTypeException {
    return solveType(type.asString());
  }

  @Override
  public void addImport(String importString) {
    this.imports.add(importString);
  }

  @Override
  public void addImports(List<String> importList) {
    this.imports.addAll(importList);
  }

  @Override
  public void setCurrentClassPath(String classPath) {

  }

  @Override
  public void reset() {
    this.imports.clear();
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

  private Optional<String> checkImports(final String typeName) {
    for (final String importEntry : this.imports) {
      if (importEntry.endsWith(typeName)) {
        return Optional.of(importEntry);
      }
    }
    return Optional.empty();
  }
}
