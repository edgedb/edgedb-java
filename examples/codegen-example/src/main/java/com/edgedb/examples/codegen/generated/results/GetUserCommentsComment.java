package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.examples.codegen.generated.interfaces.Comment;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@EdgeDBType
public final class GetUserCommentsComment implements Comment {
  @EdgeDBName("author")
  public final GetUserCommentsUser author;

  @EdgeDBName("post")
  public final GetUserCommentsPost post;

  @EdgeDBName("created_at")
  public final OffsetDateTime createdAt;

  @EdgeDBName("content")
  public final String content;

  @EdgeDBName("id")
  public final UUID id;

  @EdgeDBDeserializer
  public GetUserCommentsComment(@EdgeDBName("author") GetUserCommentsUser author,
      @EdgeDBName("post") GetUserCommentsPost post,
      @EdgeDBName("createdAt") OffsetDateTime createdAt, @EdgeDBName("content") String content,
      @EdgeDBName("id") UUID id) {
    this.author = author;
    this.post = post;
    this.createdAt = createdAt;
    this.content = content;
    this.id = id;
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
  public Optional<OffsetDateTime> getCreatedAt() {
    return Optional.of(this.createdAt);
  }

  /**
   * Returns an optional wrapping the {@code content} field, which is always present on this type.
   */
  @Override
  public Optional<String> getContent() {
    return Optional.of(this.content);
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }
}
