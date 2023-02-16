package com.edgedb.driver.exceptions;

public class ConfigurationException extends EdgeDBException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Exception cause) {
        super(message, cause);
    }
}
