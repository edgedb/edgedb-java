package com.edgedb.driver.state;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Session {
    private final String module;
    private final Map<String, String> aliases;
    private final Config config;
    private final Map<String, Object> globals;

    public Session() {
        module = "default";
        config = Config.DEFAULT;
        aliases = Map.of();
        globals = Map.of();
    }

    public Map<String, Object> serialize() {
        return new HashMap<>() {
            {
                if(!module.equals("default")) {
                    put("module", "default");
                }

                if(!aliases.isEmpty()) {
                    put("aliases", aliases);
                }

                var serializedConfig = config.serialize();
                if(!serializedConfig.isEmpty()) {
                    put("config", serializedConfig);
                }

                if(!globals.isEmpty()) {
                    put("globals", globals.entrySet().stream()
                            .map(v -> v.getKey().contains("::")
                                ? v
                                : Map.entry(module + "::" + v.getKey(), v.getValue()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    );
                }
            }
        };
    }
}
