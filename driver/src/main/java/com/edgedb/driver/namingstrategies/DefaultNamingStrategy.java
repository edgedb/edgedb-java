package com.edgedb.driver.namingstrategies;

public final class DefaultNamingStrategy implements NamingStrategy {
    public static final DefaultNamingStrategy instance = new DefaultNamingStrategy();
    @Override
    public String convert(String name) {
        return name;
    }
}
