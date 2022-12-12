package net.explorviz.code.analysis.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Exception regarding program error based on wrong property.
 */
public class PropertyFaultException extends Exception {
  public PropertyFaultException(String propertyName) {
    this(propertyName, "");
  }

  public PropertyFaultException(String propertyName, String errorMessage) {
    super(toMessage(new String[]{propertyName}, errorMessage));
  }

  public PropertyFaultException(String[] propertyNames) {
    this(propertyNames, "");
  }

  public PropertyFaultException(String[] propertyNames, String errorMessage) {
    super(PropertyFaultException.toMessage(propertyNames, errorMessage));
  }

  private static @NotNull String toMessage(String @NotNull [] propertyNames, String errorMessage) {
    StringBuilder message = new StringBuilder("Properties ");
    for (String item : propertyNames) {
      message.append("<").append(item).append("> ");
    }
    message.append(
        " is the reason for an error, check the spelling "
            + "and validate the value in application.properties\n").append(
        errorMessage);
    return message.toString();
  }
}
