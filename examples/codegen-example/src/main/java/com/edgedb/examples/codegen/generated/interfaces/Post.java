package com.edgedb.examples.codegen.generated.interfaces;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the schema type {@code codegen::Post} with properties that are shared
 *  across the following types:
 * {@linkplain com.edgedb.examples.codegen.generated.results.CreatePostPost}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsPost}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsPost}
 */
public interface Post {
  /**
   * Gets the {@code created_at} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsPost}
   */
  Optional<OffsetDateTime> getCreatedAt();

  /**
   * Gets the {@code id} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.CreatePostPost}
   */
  Optional<UUID> getId();

  /**
   * Gets the {@code title} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsPost}
   */
  Optional<String> getTitle();

  /**
   * Gets the {@code content} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsPost}
   */
  Optional<String> getContent();

  /**
   * Gets the {@code author} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost}, {@linkplain com.edgedb.examples.codegen.generated.results.GetUserPostsPost}
   */
  Optional<User> getAuthor();
}
