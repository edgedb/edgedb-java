package com.edgedb.driver.namingstrategies;

import com.edgedb.driver.util.StringsUtil;

final class SnakeCaseNamingStrategy implements NamingStrategy {
    public static final SnakeCaseNamingStrategy INSTANCE = new SnakeCaseNamingStrategy();

    @Override
    public String convert(String name) {
        if (StringsUtil.isNullOrEmpty(name)) {
            return name;
        }

        var result = new StringBuilder(String.valueOf(Character.toLowerCase(name.charAt(0))));

        for(int i = 1; i != name.length(); i++) {
            var c = name.charAt(i);

            if(Character.isUpperCase(c)) {
                result.append("_");
            }

            result.append(Character.toLowerCase(c));
        }

        return result.toString();
    }
}
