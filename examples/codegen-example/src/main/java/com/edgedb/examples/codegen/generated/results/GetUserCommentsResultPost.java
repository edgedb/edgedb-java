package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserCommentsResultPost {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("title")
  private final String title;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("author")
  private final GetUserCommentsResultPostAuthor author;

  @EdgeDBName("created_at")
  private final @Nullable OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserCommentsResultPost(@EdgeDBName("id") UUID id, @EdgeDBName("title") String title,
      @EdgeDBName("content") String content,
      @EdgeDBName("author") GetUserCommentsResultPostAuthor author,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.author = author;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return this.id;
  }

  public String getTitle() {
    return this.title;
  }

  public String getContent() {
    return this.content;
  }

  public GetUserCommentsResultPostAuthor getAuthor() {
    return this.author;
  }

  public @Nullable OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
