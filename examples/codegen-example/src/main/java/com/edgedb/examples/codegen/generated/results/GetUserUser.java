package com.edgedb.examples.codegen.generated.results;

import com.edgedb.driver.annotations.EdgeDBDeserializer;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.examples.codegen.generated.interfaces.User;
import java.lang.Override;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@EdgeDBType
public final class GetUserUser implements User {
  /**
   * The {@code name} field on the {@code codegen::User} object
   */
  @EdgeDBName("name")
  public final String name;

  /**
   * The {@code id} field on the {@code codegen::User} object
   */
  @EdgeDBName("id")
  public final UUID id;

  /**
   * The {@code joined_at} field on the {@code codegen::User} object
   */
  @EdgeDBName("joined_at")
  public final @Nullable OffsetDateTime joinedAt;

  /**
   * The {@code liked_posts} field on the {@code codegen::User} object
   */
  @EdgeDBName("liked_posts")
  public final List<GetUserPost> likedPosts;

  /**
   * The {@code posts} field on the {@code codegen::User} object
   */
  @EdgeDBName("posts")
  public final List<GetUserPost> posts;

  /**
   * The {@code comments} field on the {@code codegen::User} object
   */
  @EdgeDBName("comments")
  public final List<GetUserComment> comments;

  @EdgeDBDeserializer
  public GetUserUser(@EdgeDBName("name") String name, @EdgeDBName("id") UUID id,
      @EdgeDBName("joinedAt") @Nullable OffsetDateTime joinedAt,
      @EdgeDBName("likedPosts") List<GetUserPost> likedPosts,
      @EdgeDBName("posts") List<GetUserPost> posts,
      @EdgeDBName("comments") List<GetUserComment> comments) {
    this.name = name;
    this.id = id;
    this.joinedAt = joinedAt;
    this.likedPosts = likedPosts;
    this.posts = posts;
    this.comments = comments;
  }

  /**
   * Returns the {@code id} field of this class
   */
  @Override
  public UUID getId() {
    return this.id;
  }

  /**
   * Returns an optional wrapping the {@code joinedAt} field, which is always present on this type.
   */
  @Override
  public NullableOptional<@Nullable OffsetDateTime> getJoinedAt() {
    return NullableOptional.of(this.joinedAt);
  }

  /**
   * Returns an optional wrapping the {@code likedPosts} field, which is always present on this type.
   */
  @Override
  public Optional<List<GetUserPost>> getLikedPosts() {
    return Optional.of(this.likedPosts);
  }

  /**
   * Returns an optional wrapping the {@code name} field, which is always present on this type.
   */
  @Override
  public Optional<String> getName() {
    return Optional.of(this.name);
  }

  /**
   * Returns an optional wrapping the {@code posts} field, which is always present on this type.
   */
  @Override
  public Optional<List<GetUserPost>> getPosts() {
    return Optional.of(this.posts);
  }

  /**
   * Returns an optional wrapping the {@code comments} field, which is always present on this type.
   */
  @Override
  public Optional<List<GetUserComment>> getComments() {
    return Optional.of(this.comments);
  }
}
