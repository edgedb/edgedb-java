package com.edgedb.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.util.UUID;

public final class CreatePost.edgeqlResult {
  @EdgeDBName(&S)
  private final UUID id;

  @EdgeDBDeserializer
  CreatePost.edgeqlResult(@EdgeDBName(&S) UUID id) {
    this.&N = &N;
  }

  public UUID getId() {
    return this.&N;
  }
}