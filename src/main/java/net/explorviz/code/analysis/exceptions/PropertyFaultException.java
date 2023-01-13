package net.explorviz.code.analysis.exceptions;


/**
 * Exception regarding program error based on wrong property.
 */
public class PropertyFaultException extends Exception {

  public static final long serialVersionUID = 765345120;

  public PropertyFaultException(final String propertyName) {
    this(propertyName, "");
  }

  public PropertyFaultException(final String propertyName, final String errorMessage) {
    super(toMessage(new String[]{propertyName}, errorMessage));
  }

  public PropertyFaultException(final String[] propertyNames) { //NOPMD
    this(propertyNames, "");
  }

  public PropertyFaultException(final String[] propertyNames, final String errorMessage) {
    super(PropertyFaultException.toMessage(propertyNames, errorMessage));
  }

  private static String toMessage(final String[] propertyNames,
                                  final String errorMessage) {
    final StringBuilder message = new StringBuilder(150);
    message.append("Properties ");
    for (final String item : propertyNames) {
      message.append('<').append(item).append("> ");
    }
    message.append(
        " is the reason for an error, check the spelling "
            + "and validate the value in application.properties\n").append(
        errorMessage);
    return message.toString();
  }
}
