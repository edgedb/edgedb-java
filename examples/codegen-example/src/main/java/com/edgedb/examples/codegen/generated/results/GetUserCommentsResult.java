package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserCommentsResult {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("author")
  private final GetUserCommentsResultAuthor author;

  @EdgeDBName("post")
  private final GetUserCommentsResultPost post;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("created_at")
  private final @Nullable OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserCommentsResult(@EdgeDBName("id") UUID id,
      @EdgeDBName("author") GetUserCommentsResultAuthor author,
      @EdgeDBName("post") GetUserCommentsResultPost post, @EdgeDBName("content") String content,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt) {
    this.id = id;
    this.author = author;
    this.post = post;
    this.content = content;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return this.id;
  }

  public GetUserCommentsResultAuthor getAuthor() {
    return this.author;
  }

  public GetUserCommentsResultPost getPost() {
    return this.post;
  }

  public String getContent() {
    return this.content;
  }

  public @Nullable OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
