package com.edgedb.generated;

import com.edgedb.driver.EdgeDBQueryable;
import java.lang.String;

public final class CreatePost.edgeql {
  public static final String QUERY = &S;

  CreatePost.edgeqlResult run(EdgeDBQueryable client, String title, String content) {
    return client.&L(QUERY, new HashMap<>(){{put("title", title);; put("content", content);}}, EnumSet<Capabilities>.of(Capabilities.MODIFICATIONS, Capabilities.ALL);
  }
}
