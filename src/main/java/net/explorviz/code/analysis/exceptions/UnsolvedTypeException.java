package net.explorviz.code.analysis.exceptions;

public class UnsolvedTypeException extends Exception {

  public static final long serialVersionUID = 345758626;

  public UnsolvedTypeException(final String unsolvedType) {
    super("Could not solve type >" + unsolvedType + "<");
  }

  public UnsolvedTypeException(final String unsolvedType, final String fileName) {
    super("Could not solve type >" + unsolvedType + "< in file " + fileName);
  }

  public UnsolvedTypeException(final String unsolvedType, final String fileName, final int line) {
    super("Could not solve type >" + unsolvedType + "< in file " + fileName + " line " + line);
  }
}
