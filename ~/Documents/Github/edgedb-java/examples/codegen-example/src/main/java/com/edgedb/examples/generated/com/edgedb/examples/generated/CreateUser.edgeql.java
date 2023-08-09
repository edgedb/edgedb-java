package com.edgedb.examples.generated;

import com.edgedb.driver.EdgeDBQueryable;
import java.lang.String;

public final class CreateUser.edgeql {
  public static final String QUERY = &S;

  CreateUser.edgeqlResult run(EdgeDBQueryable client, String name) {
    return client.&L(QUERY, new HashMap<>(){{put("name", name);}}, EnumSet<Capabilities>.of(Capabilities.MODIFICATIONS, Capabilities.ALL);
  }
}
