package com.edgedb.driver.clients;

import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public interface StatefulClient {
    StatefulClient withModule(@NotNull String module);
    StatefulClient withSession(@NotNull Session session);
    StatefulClient withModuleAliases(@NotNull Map<String, String> aliases);
    StatefulClient withConfig(@NotNull Config config);
    StatefulClient withConfig(@NotNull Consumer<Config.Builder> func);
    StatefulClient withGlobals(@NotNull Map<String, Object> globals);
}
