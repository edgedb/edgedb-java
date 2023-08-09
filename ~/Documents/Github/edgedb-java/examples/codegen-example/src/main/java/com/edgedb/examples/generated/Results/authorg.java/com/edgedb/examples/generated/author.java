package com.edgedb.examples.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.lang.String;
import java.time.OffsetDateTime;

public final class author {
  @EdgeDBName(&S)
  private final String name;

  @EdgeDBName(&S)
  private final OffsetDateTime joined_at;

  @EdgeDBDeserializer
  author(@EdgeDBName(&S) String name, @EdgeDBName(&S) OffsetDateTime joined_at) {
    this.&N = &N;
    this.&N = &N;
  }

  public String getName() {
    return this.&N;
  }

  public OffsetDateTime getJoinedAt() {
    return this.&N;
  }
}
