package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@EdgeDBType
public final class GetUserResult {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("name")
  private final String name;

  @EdgeDBName("joined_at")
  private final OffsetDateTime joinedAt;

  @EdgeDBDeserializer
  public GetUserResult(@EdgeDBName("id") UUID id, @EdgeDBName("name") String name,
      @EdgeDBName("joinedAt") OffsetDateTime joinedAt) {
    this.id = id;
    this.name = name;
    this.joinedAt = joinedAt;
  }

  public UUID getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public OffsetDateTime getJoinedAt() {
    return this.joinedAt;
  }
}
