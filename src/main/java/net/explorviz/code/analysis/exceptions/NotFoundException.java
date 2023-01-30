package net.explorviz.code.analysis.exceptions;

/**
 * Exception for not found things.
 */
public class NotFoundException extends Exception {
  public static final long serialVersionUID = 987345021;

  public NotFoundException(final String reason) {
    super(reason);
  }
}
