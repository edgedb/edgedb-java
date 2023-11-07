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
public final class LikePostUser implements User {
  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public LikePostUser(@EdgeDBName("id") UUID id) {
    this.id = id;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<String> getName() {
    return Optional.empty();
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
  public Optional<OffsetDateTime> getJoinedAt() {
    return Optional.empty();
  }
}
