package com.edgedb.driver.clients;

import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;

import java.util.Map;

public interface StatefulClient extends EdgeDBQueryable {
    StatefulClient withModule(String module);
    StatefulClient withSession(Session session);
    StatefulClient withModuleAliases(Map<String, String> aliases);
    StatefulClient withConfig(Config config);
    StatefulClient withGlobals(Map<String, Object> globals);
}
