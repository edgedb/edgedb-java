package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.examples.codegen.generated.interfaces.Comment;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserComment implements Comment {
  /**
   * The {@code id} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("id")
  public final UUID id;

  /**
   * The {@code post} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("post")
  public final GetUserPost post;

  /**
   * The {@code created_at} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("created_at")
  public final @Nullable OffsetDateTime createdAt;

  /**
   * The {@code content} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("content")
  public final String content;

  @EdgeDBDeserializer
  public GetUserComment(@EdgeDBName("id") UUID id, @EdgeDBName("post") GetUserPost post,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt,
      @EdgeDBName("content") String content) {
    this.id = id;
    this.post = post;
    this.createdAt = createdAt;
    this.content = content;
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
  public Optional<User> getAuthor() {
    return Optional.empty();
  }

  /**
   * Returns an optional wrapping the {@code createdAt} field, which is always present on this type.
   */
  @Override
  public NullableOptional<@Nullable OffsetDateTime> getCreatedAt() {
    return NullableOptional.of(this.createdAt);
  }

  /**
   * Returns an optional wrapping the {@code content} field, which is always present on this type.
   */
  @Override
  public Optional<String> getContent() {
    return Optional.of(this.content);
  }

  /**
   * Returns an optional wrapping the {@code post} field, which is always present on this type.
   */
  @Override
  public Optional<Post> getPost() {
    return Optional.of(this.post);
  }
}
