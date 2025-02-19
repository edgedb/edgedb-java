package com.gel.driver.namingstrategies;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a naming strategy that can convert property names to a target format.
 */
public interface NamingStrategy {
    /**
     * Converts the given name into a new format.
     * @param name The name to convert.
     * @return The name, represented in a new format.
     */
    String convert(String name);

    /**
     * Gets a {@code camelCase} naming strategy instance.
     * @return A {@linkplain NamingStrategy} that converts strings to {@code camelCase}.
     */
    static @NotNull NamingStrategy camelCase() {
        return CamelCaseNamingStrategy.INSTANCE;
    }

    /**
     * Gets a naming strategy that does not modify the input.
     * @return A {@linkplain NamingStrategy} that does not modify the input.
     */
    static @NotNull NamingStrategy defaultStrategy() {
        return DefaultNamingStrategy.INSTANCE;
    }

    /**
     * Gets a {@code PascalCase} naming strategy instance.
     * @return A {@linkplain NamingStrategy} that converts strings to {@code PascalCase}.
     */
    static @NotNull NamingStrategy pascalCase() {
        return PascalNamingStrategy.INSTANCE;
    }

    /**
     * Gets a {@code snake_case} naming strategy instance.
     * @return A {@linkplain NamingStrategy} that converts strings to {@code snake_case}.
     */
    static @NotNull NamingStrategy snakeCase() {
        return SnakeCaseNamingStrategy.INSTANCE;
    }
}
