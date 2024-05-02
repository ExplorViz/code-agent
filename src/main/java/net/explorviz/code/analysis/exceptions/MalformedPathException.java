package net.explorviz.code.analysis.exceptions;

/**
 * Exception for malformed paths.
 */
public class MalformedPathException extends RuntimeException {

  public static final long serialVersionUID = 265745021;

  public MalformedPathException(final String reason) {
    super(reason);
  }
}
