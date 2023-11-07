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
public final class GetUserLikedPostsPost implements Post {
  @EdgeDBName("title")
  public final String title;

  @EdgeDBName("author")
  public final GetUserLikedPostsUser author;

  @EdgeDBName("content")
  public final String content;

  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public GetUserLikedPostsPost(@EdgeDBName("title") String title,
      @EdgeDBName("author") GetUserLikedPostsUser author, @EdgeDBName("content") String content,
      @EdgeDBName("createdAt") OffsetDateTime createdAt, @EdgeDBName("id") UUID id) {
    this.title = title;
    this.author = author;
    this.content = content;
    this.createdAt = createdAt;
    this.id = id;
  }

  /**
   * Returns an optional wrapping the {@code title} field, which is always present on this type.
   */
  @Override
  public Optional<String> getTitle() {
    return Optional.of(this.title);
  }

  /**
   * Returns an optional wrapping the {@code author} field, which is always present on this type.
   */
  @Override
  public Optional<User> getAuthor() {
    return Optional.of(this.author);
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
   * Returns an optional wrapping the {@code createdAt} field, which is always present on this type.
   */
  @Override
  public Optional<OffsetDateTime> getCreatedAt() {
    return Optional.of(this.createdAt);
  }
}
