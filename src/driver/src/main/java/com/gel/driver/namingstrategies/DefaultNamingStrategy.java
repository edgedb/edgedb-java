package com.gel.driver.namingstrategies;

final class DefaultNamingStrategy implements NamingStrategy {
    public static final DefaultNamingStrategy INSTANCE = new DefaultNamingStrategy();
    @Override
    public String convert(String name) {
        return name;
    }
}
