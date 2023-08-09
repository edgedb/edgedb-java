package com.edgedb.examples.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.util.UUID;

public final class LikePost.edgeqlResult {
  @EdgeDBName(&S)
  private final UUID id;

  @EdgeDBDeserializer
  LikePost.edgeqlResult(@EdgeDBName(&S) UUID id) {
    this.&N = &N;
  }

  public UUID getId() {
    return this.&N;
  }
}
