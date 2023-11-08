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
public final class GetUserCommentsComment implements Comment {
  /**
   * The {@code content} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("content")
  public final String content;

  /**
   * The {@code author} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("author")
  public final GetUserCommentsUser author;

  /**
   * The {@code post} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("post")
  public final GetUserCommentsPost post;

  /**
   * The {@code id} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("id")
  public final UUID id;

  /**
   * The {@code created_at} field on the {@code codegen::Comment} object
   */
  @EdgeDBName("created_at")
  public final @Nullable OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserCommentsComment(@EdgeDBName("content") String content,
      @EdgeDBName("author") GetUserCommentsUser author,
      @EdgeDBName("post") GetUserCommentsPost post, @EdgeDBName("id") UUID id,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt) {
    this.content = content;
    this.author = author;
    this.post = post;
    this.id = id;
    this.createdAt = createdAt;
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }

  /**
   * Returns an optional wrapping the {@code content} field, which is always present on this type.
   */
  @Override
  public Optional<String> getContent() {
    return Optional.of(this.content);
  }

  /**
   * Returns an optional wrapping the {@code author} field, which is always present on this type.
   */
  @Override
  public Optional<User> getAuthor() {
    return Optional.of(this.author);
  }

  /**
   * Returns an optional wrapping the {@code post} field, which is always present on this type.
   */
  @Override
  public Optional<Post> getPost() {
    return Optional.of(this.post);
  }

  /**
   * Returns an optional wrapping the {@code createdAt} field, which is always present on this type.
   */
  @Override
  public NullableOptional<@Nullable OffsetDateTime> getCreatedAt() {
    return NullableOptional.of(this.createdAt);
  }
}
