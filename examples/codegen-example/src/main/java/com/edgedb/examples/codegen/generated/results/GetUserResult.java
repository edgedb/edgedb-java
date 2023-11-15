package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserResult {
  @EdgeDBName("id")
  private final UUID id;

  @EdgeDBName("name")
  private final String name;

  @EdgeDBName("joined_at")
  private final @Nullable OffsetDateTime joinedAt;

  @EdgeDBName("liked_posts")
  private final List<GetUserResultLikedPosts> likedPosts;

  @EdgeDBName("posts")
  private final List<GetUserResultPosts> posts;

  @EdgeDBName("comments")
  private final List<GetUserResultComments> comments;

  @EdgeDBDeserializer
  public GetUserResult(@EdgeDBName("id") UUID id, @EdgeDBName("name") String name,
      @EdgeDBName("joinedAt") @Nullable OffsetDateTime joinedAt,
      @EdgeDBName("likedPosts") List<GetUserResultLikedPosts> likedPosts,
      @EdgeDBName("posts") List<GetUserResultPosts> posts,
      @EdgeDBName("comments") List<GetUserResultComments> comments) {
    this.id = id;
    this.name = name;
    this.joinedAt = joinedAt;
    this.likedPosts = likedPosts;
    this.posts = posts;
    this.comments = comments;
  }

  public UUID getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public @Nullable OffsetDateTime getJoinedAt() {
    return this.joinedAt;
  }

  public List<GetUserResultLikedPosts> getLikedPosts() {
    return this.likedPosts;
  }

  public List<GetUserResultPosts> getPosts() {
    return this.posts;
  }

  public List<GetUserResultComments> getComments() {
    return this.comments;
  }
}
