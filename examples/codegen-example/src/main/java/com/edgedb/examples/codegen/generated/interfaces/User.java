package com.edgedb.examples.codegen.generated.interfaces;

import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.examples.codegen.generated.results.GetUserComment;
import com.edgedb.examples.codegen.generated.results.GetUserPost;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the schema type {@code codegen::User} with properties that are shared
 *  across the following types:
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.LikePostUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.CreateUserUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsUser}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsUser}
 */
public interface User {
  /**
   * Gets the {@code id} property, available on all descendants of this interface.
   */
  UUID getId();

  /**
   * Gets the {@code joined_at} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsUser}
   */
  NullableOptional<@Nullable OffsetDateTime> getJoinedAt();

  /**
   * Gets the {@code liked_posts} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}
   */
  Optional<List<GetUserPost>> getLikedPosts();

  /**
   * Gets the {@code name} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsUser}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsUser}
   */
  Optional<String> getName();

  /**
   * Gets the {@code posts} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}
   */
  Optional<List<GetUserPost>> getPosts();

  /**
   * Gets the {@code comments} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserUser}
   */
  Optional<List<GetUserComment>> getComments();
}
