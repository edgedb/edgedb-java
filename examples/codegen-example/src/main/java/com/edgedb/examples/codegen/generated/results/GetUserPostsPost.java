package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@EdgeDBType
public final class GetUserPostsPost implements Post {
  @EdgeDBName("title")
  public final String title;

  @EdgeDBName("author")
  public final GetUserPostsUser author;

  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("content")
  public final String content;

  @EdgeDBDeserializer
  public GetUserPostsPost(@EdgeDBName("title") String title,
      @EdgeDBName("author") GetUserPostsUser author,
      @EdgeDBName("createdAt") OffsetDateTime createdAt, @EdgeDBName("content") String content) {
    this.title = title;
    this.author = author;
    this.createdAt = createdAt;
    this.content = content;
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
  public Optional<GetUserCommentsUser> getAuthor() {
    return this.author;
  }
}
