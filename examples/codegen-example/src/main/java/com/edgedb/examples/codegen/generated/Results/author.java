package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;

@EdgeDBType
public final class author {
  @EdgeDBName("name")
  private final String name;

  @EdgeDBName("joined_at")
  private final OffsetDateTime joinedAt;

  @EdgeDBDeserializer
  public author(@EdgeDBName("name") String name, @EdgeDBName("joinedAt") OffsetDateTime joinedAt) {
    this.name = name;
    this.joinedAt = joinedAt;
  }

  public String getName() {
    return this.name;
  }

  public OffsetDateTime getJoinedAt() {
    return this.joinedAt;
  }
}
