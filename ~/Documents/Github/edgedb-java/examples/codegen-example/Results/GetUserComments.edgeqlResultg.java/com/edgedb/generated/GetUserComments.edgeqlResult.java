package com.edgedb.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.lang.String;
import java.time.OffsetDateTime;

public final class GetUserComments.edgeqlResult {
  @EdgeDBName(&S)
  private final author author;

  @EdgeDBName(&S)
  private final post post;

  @EdgeDBName(&S)
  private final String content;

  @EdgeDBName(&S)
  private final OffsetDateTime created_at;

  @EdgeDBDeserializer
  GetUserComments.edgeqlResult(@EdgeDBName(&S) author author, @EdgeDBName(&S) post post,
      @EdgeDBName(&S) String content, @EdgeDBName(&S) OffsetDateTime created_at) {
    this.&N = &N;
    this.&N = &N;
    this.&N = &N;
    this.&N = &N;
  }

  public author getAuthor() {
    return this.&N;
  }

  public post getPost() {
    return this.&N;
  }

  public String getContent() {
    return this.&N;
  }

  public OffsetDateTime getCreatedAt() {
    return this.&N;
  }
}
