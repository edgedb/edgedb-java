package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@EdgeDBType
public final class GetUserCommentsPost implements Post {
  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("title")
  public final String title;

  @EdgeDBName("content")
  public final String content;

  @EdgeDBName("author")
  public final GetUserCommentsUser author;

  @EdgeDBDeserializer
  public GetUserCommentsPost(@EdgeDBName("id") UUID id,
      @EdgeDBName("createdAt") OffsetDateTime createdAt, @EdgeDBName("title") String title,
      @EdgeDBName("content") String content, @EdgeDBName("author") GetUserCommentsUser author) {
    this.id = id;
    this.createdAt = createdAt;
    this.title = title;
    this.content = content;
    this.author = author;
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }

  /**
   * Returns an optional wrapping the {@code createdAt} field, which is always present on this type.
   */
  @Override
  public Optional<OffsetDateTime> getCreatedAt() {
    return Optional.of(this.createdAt);
  }

  /**
   * Returns an optional wrapping the {@code title} field, which is always present on this type.
   */
  @Override
  public Optional<String> getTitle() {
    return Optional.of(this.title);
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
}
