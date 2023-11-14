package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserPostsUser implements User {
  /**
   * The {@code name} field on the {@code codegen::User} object
   */
  @EdgeDBName("name")
  public final String name;

  /**
   * The {@code joined_at} field on the {@code codegen::User} object
   */
  @EdgeDBName("joined_at")
  public final @Nullable OffsetDateTime joinedAt;

  /**
   * The {@code id} field on the {@code codegen::User} object
   */
  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public GetUserPostsUser(@EdgeDBName("name") String name,
      @EdgeDBName("joinedAt") @Nullable OffsetDateTime joinedAt, @EdgeDBName("id") UUID id) {
    this.name = name;
    this.joinedAt = joinedAt;
    this.id = id;
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
  public NullableOptional<@Nullable OffsetDateTime> getJoinedAt() {
    return NullableOptional.of(this.joinedAt);
  }

  /**
   * Returns an optional wrapping the {@code name} field, which is always present on this type.
   */
  @Override
  public Optional<String> getName() {
    return Optional.of(this.name);
  }
}
