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
  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("title")
  public final String title;

  @EdgeDBName("content")
  public final String content;

  @EdgeDBName("author")
  public final GetUserCommentsUser author;

  @EdgeDBDeserializer
  public GetUserCommentsPost(@EdgeDBName("createdAt") OffsetDateTime createdAt,
      @EdgeDBName("title") String title, @EdgeDBName("content") String content,
      @EdgeDBName("author") GetUserCommentsUser author) {
    this.createdAt = createdAt;
    this.title = title;
    this.content = content;
    this.author = author;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<OffsetDateTime> getCreatedAt() {
    return Optional.empty();
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<UUID> getId() {
    return Optional.empty();
  }

  /**
   * Returns the {@code title} field of this class
   */
  @Override
  public Optional<String> getTitle() {
    return this.title;
  }

  /**
   * Returns the {@code content} field of this class
   */
  @Override
  public Optional<String> getContent() {
    return this.content;
  }

  /**
   * Returns the {@code author} field of this class
   */
  @Override
  public Optional<User> getAuthor() {
    return Optional.of(this.author);
  }
}
