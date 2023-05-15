package com.edgedb.driver.namingstrategies;

public interface NamingStrategy {
    String convert(String name);

    static NamingStrategy camelCase() {
        return CamelCaseNamingStrategy.instance;
    }

    static NamingStrategy defaultStrategy() {
        return DefaultNamingStrategy.instance;
    }

    static NamingStrategy pascalCase() {
        return PascalNamingStrategy.instance;
    }

    static NamingStrategy snakeCase() {
        return SnakeCaseNamingStrategy.instance;
    }
}
