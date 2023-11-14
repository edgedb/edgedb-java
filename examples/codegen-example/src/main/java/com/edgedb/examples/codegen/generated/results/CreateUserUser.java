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
public final class CreateUserUser implements User {
  /**
   * The {@code id} field on the {@code codegen::User} object
   */
  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public CreateUserUser(@EdgeDBName("id") UUID id) {
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
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public NullableOptional<@Nullable OffsetDateTime> getJoinedAt() {
    return NullableOptional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<String> getName() {
    return Optional.empty();
  }
}
