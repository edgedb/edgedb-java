package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserLikedPostsResultAuthor {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("name")
  private final String name;

  @EdgeDBName("joined_at")
  private final @Nullable OffsetDateTime joinedAt;

  @EdgeDBDeserializer
  public GetUserLikedPostsResultAuthor(@EdgeDBName("id") UUID id, @EdgeDBName("name") String name,
      @EdgeDBName("joinedAt") @Nullable OffsetDateTime joinedAt) {
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

  public @Nullable OffsetDateTime getJoinedAt() {
    return this.joinedAt;
  }
}
