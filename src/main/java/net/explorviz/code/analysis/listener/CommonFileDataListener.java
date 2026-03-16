package net.explorviz.code.analysis.listener;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Common methods for file data listeners.
 */
public interface CommonFileDataListener {

  default int calculateLoc(final ParserRuleContext ctx) {
    if (ctx == null || ctx.start == null || ctx.stop == null) {
      return 0;
    }
    return ctx.stop.getLine() - ctx.start.getLine() + 1;
  }

  default int getLoc(final ParserRuleContext ctx) {
    if (ctx == null || ctx.stop == null) {
      return 0;
    }
    if (ctx.start != null) {
      return ctx.stop.getLine() - ctx.start.getLine() + 1;
    }
    return ctx.stop.getLine();
  }

  default String getClassPathFromFqn(final String fqn, final String fileExtension,
      final String currentFilePath, final String currentPackage) {
    if (fqn == null || fqn.isEmpty()) {
      return "unknown/file";
    }

    String baseFqn = fqn;

    // Clean up fqn to prepare for path conversion
    final int genericIdxAngle = baseFqn.indexOf('<');
    if (genericIdxAngle != -1) {
      baseFqn = baseFqn.substring(0, genericIdxAngle);
    }
    final int genericIdxSquare = baseFqn.indexOf('[');
    if (genericIdxSquare != -1) {
      baseFqn = baseFqn.substring(0, genericIdxSquare);
    }
    baseFqn = baseFqn.replace("[]", "");

    // Convert fqn to path
    final int lastDot = baseFqn.lastIndexOf('.');
    final String fqnPath;
    if (lastDot != -1) {
      fqnPath = baseFqn.replace('.', '/') + fileExtension;
    } else {
      fqnPath = baseFqn + fileExtension;
    }

    // Use current file path to best guess path prefix of given class
    String pathPrefix = "";
    if (currentFilePath != null && currentPackage != null && !currentPackage.isEmpty()) {
      final String packagePath = currentPackage.replace('.', '/');
      final int packageIdx = currentFilePath.lastIndexOf(packagePath);
      if (packageIdx != -1) {
        pathPrefix = currentFilePath.substring(0, packageIdx);
      }
    } else if (currentFilePath != null && currentFilePath.contains("/")) {
      pathPrefix = currentFilePath.substring(0, currentFilePath.lastIndexOf("/") + 1);
    }

    // Return (best guess) path for given fqn
    return pathPrefix + fqnPath;
  }

  default String getClassNameFromFqn(final String fqn) {
    if (fqn == null || fqn.isEmpty()) {
      return "unknown";
    }

    String baseFqn = fqn;
    final int genericIdxAngle = baseFqn.indexOf('<');
    if (genericIdxAngle != -1) {
      baseFqn = baseFqn.substring(0, genericIdxAngle);
    }
    final int genericIdxSquare = baseFqn.indexOf('[');
    if (genericIdxSquare != -1) {
      baseFqn = baseFqn.substring(0, genericIdxSquare);
    }

    final int lastDot = baseFqn.lastIndexOf('.');
    if (lastDot != -1) {
      return baseFqn.substring(lastDot + 1);
    }

    return baseFqn;
  }
}
