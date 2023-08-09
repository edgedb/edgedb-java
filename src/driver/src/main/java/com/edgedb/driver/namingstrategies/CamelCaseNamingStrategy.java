package com.edgedb.driver.namingstrategies;

import com.edgedb.driver.util.StringsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class CamelCaseNamingStrategy implements NamingStrategy {
    public static final CamelCaseNamingStrategy INSTANCE = new CamelCaseNamingStrategy();

    @Override
    public @NotNull String convert(@NotNull String name) {
        if (StringsUtil.isNullOrEmpty(name)) {
            return name;
        }

        var pascal = NamingStrategy.pascalCase().convert(name);

        return pascal.substring(0,1).toLowerCase(Locale.ROOT) + pascal.substring(1);
    }
}
