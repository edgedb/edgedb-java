package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserResultComments {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("content")
  private final String content;

  @EdgeDBName("post")
  private final GetUserResultCommentsPost post;

  @EdgeDBName("created_at")
  private final @Nullable OffsetDateTime createdAt;

  @EdgeDBDeserializer
  public GetUserResultComments(@EdgeDBName("id") UUID id, @EdgeDBName("content") String content,
      @EdgeDBName("post") GetUserResultCommentsPost post,
      @EdgeDBName("createdAt") @Nullable OffsetDateTime createdAt) {
    this.id = id;
    this.content = content;
    this.post = post;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return this.id;
  }

  public String getContent() {
    return this.content;
  }

  public GetUserResultCommentsPost getPost() {
    return this.post;
  }

  public @Nullable OffsetDateTime getCreatedAt() {
    return this.createdAt;
  }
}
