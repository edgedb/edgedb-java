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
   * Returns an optional wrapping the {@code name} field, which is always present on this type.
   */
  @Override
  public Optional<String> getName() {
    return Optional.of(this.name);
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }

  /**
   * Returns an optional wrapping the {@code joinedAt} field, which is always present on this type.
   */
  @Override
  public Optional<OffsetDateTime> getJoinedAt() {
    return Optional.of(this.joinedAt);
  }
}
