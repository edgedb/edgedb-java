package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserPostsResult {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("title")
  private final String title;

  @EdgeDBName("author")
  private final GetUserPostsResultAuthor author;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("created_at")
  private final @Nullable OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserPostsResult(@EdgeDBName("id") UUID id, @EdgeDBName("title") String title,
      @EdgeDBName("author") GetUserPostsResultAuthor author, @EdgeDBName("content") String content,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.content = content;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return this.id;
  }

  public String getTitle() {
    return this.title;
  }

  public GetUserPostsResultAuthor getAuthor() {
    return this.author;
  }

  public String getContent() {
    return this.content;
  }

  public @Nullable OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
