package com.gel.driver.exceptions;

/**
 * Represents a generic configuration error.
 */
public class ConfigurationException extends GelException {
    /**
     * Constructs a new {@linkplain ConfigurationException}.
     * @param message The configuration error message.
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@linkplain ConfigurationException}.
     * @param message The configuration error message.
     * @param cause The cause of the configuration error.
     */
    public ConfigurationException(String message, Exception cause) {
        super(message, cause);
    }
}
