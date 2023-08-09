package com.edgedb.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class GetUser.edgeqlResult {
  @EdgeDBName(&S)
  private final UUID id;

  @EdgeDBName(&S)
  private final String name;

  @EdgeDBName(&S)
  private final OffsetDateTime joined_at;

  @EdgeDBDeserializer
  GetUser.edgeqlResult(@EdgeDBName(&S) UUID id, @EdgeDBName(&S) String name,
      @EdgeDBName(&S) OffsetDateTime joined_at) {
    this.&N = &N;
    this.&N = &N;
    this.&N = &N;
  }

  public UUID getId() {
    return this.&N;
  }

  public String getName() {
    return this.&N;
  }

  public OffsetDateTime getJoinedAt() {
    return this.&N;
  }
}
