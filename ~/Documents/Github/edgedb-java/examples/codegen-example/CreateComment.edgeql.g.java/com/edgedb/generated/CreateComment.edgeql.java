package com.edgedb.generated;

import com.edgedb.driver.EdgeDBQueryable;
import java.lang.String;
import java.util.UUID;

public final class CreateComment.edgeql {
  public static final String QUERY = &S;

  CreateComment.edgeqlResult run(EdgeDBQueryable client, UUID post_id, String content) {
    return client.&L(QUERY, new HashMap<>(){{put("post_id", post_id);; put("content", content);}}, EnumSet<Capabilities>.of(Capabilities.MODIFICATIONS, Capabilities.ALL);
  }
}
