package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;

@EdgeDBType
public final class GetUserPostsResult {
  @EdgeDBName("title")
  private final String title;

  @EdgeDBName("author")
  private final author author;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("created_at")
  private final OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserPostsResult(@EdgeDBName("title") String title, @EdgeDBName("author") author author,
      @EdgeDBName("content") String content, @EdgeDBName("createdAt") OffsetDateTime createdAt) {
    this.title = title;
    this.author = author;
    this.content = content;
    this.createdAt = createdAt;
  }

  public String getTitle() {
    return this.title;
  }

  public author getAuthor() {
    return this.author;
  }

  public String getContent() {
    return this.content;
  }

  public OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
