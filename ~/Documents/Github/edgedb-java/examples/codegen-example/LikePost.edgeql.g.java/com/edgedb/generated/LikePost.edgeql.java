package com.edgedb.generated;

import com.edgedb.driver.EdgeDBQueryable;
import java.lang.String;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class LikePost.edgeql {
  public static final String QUERY = &S;

  LikePost. @Nullable edgeqlResult run(EdgeDBQueryable client, UUID post_id) {
    return client.&L(QUERY, new HashMap<>(){{put("post_id", post_id);}}, EnumSet<Capabilities>.of(Capabilities.MODIFICATIONS, Capabilities.ALL);
  }
}
