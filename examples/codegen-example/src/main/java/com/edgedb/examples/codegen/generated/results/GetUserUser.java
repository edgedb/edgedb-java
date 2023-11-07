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
public final class GetUserUser implements User {
  @EdgeDBName("name")
  public final String name;

  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBName("joined_at")
  public final OffsetDateTime joinedAt;

  @EdgeDBDeserializer
  public GetUserUser(@EdgeDBName("name") String name, @EdgeDBName("id") UUID id,
      @EdgeDBName("joinedAt") OffsetDateTime joinedAt) {
    this.name = name;
    this.id = id;
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
   * Returns the {@code id} field of this class
   */
  @Override
  public Optional<UUID> getId() {
    return this.id;
  }
}
