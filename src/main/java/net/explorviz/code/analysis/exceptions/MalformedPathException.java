package net.explorviz.code.analysis.exceptions;

public class MalformedPathException extends RuntimeException {
  public static final long serialVersionUID = 265745021;

  public MalformedPathException(final String reason) {
    super(reason);
  }
}
