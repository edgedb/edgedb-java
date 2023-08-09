package com.edgedb.examples.generated;

import com.edgedb.driver.EdgeDBQueryable;
import java.lang.String;
import org.jetbrains.annotations.Nullable;

public final class GetUser.edgeql {
  public static final String QUERY = &S;

  GetUser. @Nullable edgeqlResult run(EdgeDBQueryable client, String name) {
    return client.&L(QUERY, new HashMap<>(){{put("name", name);}}, EnumSet<Capabilities>.of();
  }
}
