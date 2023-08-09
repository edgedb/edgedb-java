package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;

@EdgeDBType
public final class GetUserLikedPostsResult {
  @EdgeDBName("title")
  private final String title;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("author")
  private final author author;

  @EdgeDBName("created_at")
  private final OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserLikedPostsResult(@EdgeDBName("title") String title,
      @EdgeDBName("content") String content, @EdgeDBName("author") author author,
      @EdgeDBName("createdAt") OffsetDateTime createdAt) {
    this.title = title;
    this.content = content;
    this.author = author;
    this.createdAt = createdAt;
  }

  public String getTitle() {
    return this.title;
  }

  public String getContent() {
    return this.content;
  }

  public author getAuthor() {
    return this.author;
  }

  public OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
