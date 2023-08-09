package com.edgedb.generated;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import java.lang.String;
import java.time.OffsetDateTime;

public final class GetUserPosts.edgeqlResult {
  @EdgeDBName(&S)
  private final String title;

  @EdgeDBName(&S)
  private final author author;

  @EdgeDBName(&S)
  private final String content;

  @EdgeDBName(&S)
  private final OffsetDateTime created_at;

  @EdgeDBDeserializer
  GetUserPosts.edgeqlResult(@EdgeDBName(&S) String title, @EdgeDBName(&S) author author,
      @EdgeDBName(&S) String content, @EdgeDBName(&S) OffsetDateTime created_at) {
    this.&N = &N;
    this.&N = &N;
    this.&N = &N;
    this.&N = &N;
  }

  public String getTitle() {
    return this.&N;
  }

  public author getAuthor() {
    return this.&N;
  }

  public String getContent() {
    return this.&N;
  }

  public OffsetDateTime getCreatedAt() {
    return this.&N;
  }
}
