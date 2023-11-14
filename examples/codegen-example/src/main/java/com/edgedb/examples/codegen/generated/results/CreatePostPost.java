package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class CreatePostPost implements Post {
  /**
   * The {@code id} field on the {@code codegen::Post} object
   */
  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public CreatePostPost(@EdgeDBName("id") UUID id) {
    this.id = id;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public NullableOptional<@Nullable OffsetDateTime> getCreatedAt() {
    return NullableOptional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<User> getAuthor() {
    return Optional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<String> getTitle() {
    return Optional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<String> getContent() {
    return Optional.empty();
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }
}
