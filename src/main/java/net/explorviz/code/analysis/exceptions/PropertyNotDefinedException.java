package net.explorviz.code.analysis.exceptions;

import java.util.List;

public class PropertyNotDefinedException extends Exception{
    public PropertyNotDefinedException(String propertyName) {
        this(propertyName, "");
    }

    public PropertyNotDefinedException(String propertyName, String errorMessage) {
        super("Property <" + propertyName + "> not defined in application.properties\n" + errorMessage);
    }

    public PropertyNotDefinedException(String[] propertyNames) {
        this(propertyNames, "");
    }

    public PropertyNotDefinedException(String[] propertyNames, String errorMessage) {
        super(PropertyNotDefinedException.toMessage(propertyNames, errorMessage));
    }

    private static String toMessage(String[] propertyNames, String errorMessage) {
        StringBuilder message = new StringBuilder("Properties ");
        for (String item:propertyNames) {
            message.append("<").append(item).append("> ");
        }
        message.append("> not defined in application.properties\n").append(errorMessage);
        return message.toString();
    }
}
