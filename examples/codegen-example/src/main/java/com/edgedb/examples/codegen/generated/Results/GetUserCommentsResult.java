package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;

@EdgeDBType
public final class GetUserCommentsResult {
  @EdgeDBName("author")
  private final author author;

  @EdgeDBName("post")
  private final post post;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("created_at")
  private final OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserCommentsResult(@EdgeDBName("author") author author, @EdgeDBName("post") post post,
      @EdgeDBName("content") String content, @EdgeDBName("createdAt") OffsetDateTime createdAt) {
    this.author = author;
    this.post = post;
    this.content = content;
    this.createdAt = createdAt;
  }

  public author getAuthor() {
    return this.author;
  }

  public post getPost() {
    return this.post;
  }

  public String getContent() {
    return this.content;
  }

  public OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
