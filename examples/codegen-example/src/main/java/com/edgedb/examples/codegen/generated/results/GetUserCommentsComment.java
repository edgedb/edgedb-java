package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.examples.codegen.generated.interfaces.Comment;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@EdgeDBType
public final class GetUserCommentsComment implements Comment {
  @EdgeDBName("content")
  public final String content;

  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("post")
  public final GetUserCommentsPost post;

  @EdgeDBName("author")
  public final GetUserCommentsUser author;

  @EdgeDBDeserializer
  public GetUserCommentsComment(@EdgeDBName("content") String content,
      @EdgeDBName("createdAt") OffsetDateTime createdAt,
      @EdgeDBName("post") GetUserCommentsPost post,
      @EdgeDBName("author") GetUserCommentsUser author) {
    this.content = content;
    this.createdAt = createdAt;
    this.post = post;
    this.author = author;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<UUID> getId() {
    return Optional.empty();
  }

  /**
   * Returns the {@code content} field of this class
   */
  @Override
  public Optional<String> getContent() {
    return this.content;
  }

  /**
   * Returns an optional whose value isn't present on the current class
   */
  @Override
  public Optional<OffsetDateTime> getCreatedAt() {
    return Optional.empty();
  }

  /**
   * Returns the {@code post} field of this class
   */
  @Override
  public Optional<GetUserCommentsPost> getPost() {
    return this.post;
  }

  /**
   * Returns the {@code author} field of this class
   */
  @Override
  public Optional<GetUserCommentsUser> getAuthor() {
    return this.author;
  }
}
