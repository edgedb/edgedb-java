package com.edgedb.driver.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Represents session-level parameters
 */
public final class Session {
    /**
     * Gets the default session-level parameters.
     */
    public static final Session DEFAULT = new Session();

    /**
     * Gets a builder used to construct a {@linkplain Session}.
     * @return A new session builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    private final String module;
    private final Map<String, String> aliases;
    private final Config config;
    private final Map<String, Object> globals;

    /**
     * Constructs a new {@linkplain Session}.
     */
    public Session() {
        module = "default";
        config = Config.DEFAULT;
        aliases = Map.of();
        globals = Map.of();
    }

    /**
     * Constructs a new {@linkplain Session}.
     * @param module The module for this session.
     * @param aliases The module aliases for this session.
     * @param config The configuration for this session.
     * @param globals The global variables for this session.
     */
    public Session(String module, Map<String, String> aliases, Config config, Map<String, Object> globals) {
        this.module = module;
        this.aliases = aliases;
        this.config = config;
        this.globals = globals;
    }

    /**
     * Gets the module of this session.
     * @return The module for this session.
     */
    public String getModule() {
        return module;
    }

    /**
     * Gets the module aliases of this session.
     * @return The module aliases for this session.
     */
    public Map<String, String> getAliases() {
        return Map.copyOf(aliases);
    }

    /**
     * Gets the configuration of this session.
     * @return The session-level configuration for this session.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Gets the global variables of this session.
     * @return The global variables for this session.
     */
    public Map<String, Object> getGlobals() {
        return Map.copyOf(globals);
    }

    /**
     * Serializes this session object to a sparse map.
     * @return A map containing the field values, if present.
     */
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

    /**
     * Clones the current session with the specified globals.
     * @param globals The global variables for the new session.
     * @return A copy of the current session with the applied global variables.
     */
    public Session withGlobals(Map<String, Object> globals) {
        return new Session(
                this.module,
                this.aliases,
                this.config,
                globals
        );
    }

    /**
     * Clones the current session with the specified module aliases
     * @param aliases The module aliases for the new session
     * @return A copy of the current session with the applied module aliases.
     */
    public Session withModuleAliases(Map<String, String> aliases) {
        return new Session(
                this.module,
                aliases,
                this.config,
                this.globals
        );
    }

    /**
     * Clones the current session with the specified session-level configuration.
     * @param config The configuration for the new session.
     * @return A copy of the current session with the applied configuration.
     */
    public Session withConfig(Config config) {
        return new Session(
                this.module,
                this.aliases,
                config,
                this.globals
        );
    }

    /**
     * Clones the current session with the specified session-level configuration builder.
     * @param func A consumer that populates a configuration builder for the new session.
     * @return A copy of the current session with the applied configuration.
     */
    public Session withConfig(Consumer<Config.Builder> func) {
        var builder = Config.builder();
        func.accept(builder);
        return new Session(
                this.module,
                this.aliases,
                builder.build(),
                this.globals
        );
    }

    /**
     * Clones the current session with the specified module.
     * @param module The module for the new session.
     * @return A copy of the current session with the applied module.
     */
    public Session withModule(String module) {
        return new Session(
                module,
                this.aliases,
                this.config,
                this.globals
        );
    }

    /**
     * Represents a builder used to construct {@linkplain Session}s.
     */
    public static final class Builder {
        private String module;
        private Map<String, String> aliases;
        private Config config;
        private Map<String, Object> globals;

        /**
         * Sets the module for this builder.
         * @param module The new module value.
         * @return The current builder.
         */
        public Builder withModule(String module) {
            this.module = module;
            return this;
        }

        /**
         * Sets the module aliases for this builder.
         * @param aliases The new module aliases.
         * @return The current builder.
         */
        public Builder withAliases(Map<String, String> aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * Sets the session-level configuration for this builder.
         * @param config The new configuration.
         * @return The current builder.
         */
        public Builder withConfig(Config config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the session-level configuration for this builder.
         * @param func A consumer that populates a configuration builder for the new session.
         * @return The current builder.
         */
        public Builder withConfig(Consumer<Config.Builder> func) {
            var builder = Config.builder();
            func.accept(builder);
            return withConfig(builder.build());
        }

        /**
         * Sets the global variables for this builder.
         * @param globals The new global variables.
         * @return The current builder.
         */
        public Builder withGlobals(Map<String, Object> globals) {
            this.globals = globals;
            return this;
        }

        /**
         * Constructs a new {@linkplain Session} from this builder.
         * @return A {@linkplain Session} with the values specified in this builder.
         */
        public Session build() {
            return new Session(module, aliases, config, globals);
        }
    }
}
