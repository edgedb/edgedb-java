package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@EdgeDBType
public final class GetUserLikedPostsUser implements User {
  @EdgeDBName("name")
  public final String name;

  @EdgeDBName("joined_at")
  public final OffsetDateTime joinedAt;

  @EdgeDBDeserializer
  public GetUserLikedPostsUser(@EdgeDBName("name") String name,
      @EdgeDBName("joinedAt") OffsetDateTime joinedAt) {
    this.name = name;
    this.joinedAt = joinedAt;
  }

  /**
   * Returns the {@code name} field of this class
   */
  @Override
  public Optional<String> getName() {
    return this.name;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<OffsetDateTime> getJoinedAt() {
    return Optional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<UUID> getId() {
    return Optional.empty();
  }
}
