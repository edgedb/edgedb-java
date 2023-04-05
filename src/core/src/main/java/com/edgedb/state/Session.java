package com.edgedb.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class Session {
    public static final Session DEFAULT = new Session();

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

    public Session(String module, Map<String, String> aliases, Config config, Map<String, Object> globals) {
        this.module = module;
        this.aliases = aliases;
        this.config = config;
        this.globals = globals;
    }

    public Map<String, Object> serialize() {
        return new HashMap<>() {
            {
                if(!module.equals("default")) {
                    put("module", module);
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

    public Session withGlobals(Map<String, Object> globals) {
        return new Session(
                this.module,
                this.aliases,
                this.config,
                globals
        );
    }

    public Session withModuleAliases(Map<String, String> aliases) {
        return new Session(
                this.module,
                aliases,
                this.config,
                this.globals
        );
    }

    public Session withConfig(Config config) {
        return new Session(
                this.module,
                this.aliases,
                config,
                this.globals
        );
    }

    public Session withConfig(Consumer<ConfigBuilder> func) {
        var builder = new ConfigBuilder();
        func.accept(builder);
        return new Session(
                this.module,
                this.aliases,
                builder.build(),
                this.globals
        );
    }

    public Session withModule(String module) {
        return new Session(
                module,
                this.aliases,
                this.config,
                this.globals
        );
    }
}
