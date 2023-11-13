package com.edgedb.examples.codegen.generated.interfaces;

import com.edgedb.driver.datatypes.NullableOptional;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the schema type {@code codegen::Comment} with properties that are shared
 *  across the following types:
 * {@linkplain com.edgedb.examples.codegen.generated.results.CreateCommentComment}
 * {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsComment}
 */
public interface Comment {
  /**
   * Gets the {@code id} property, available on all descendants of this interface.
   */
  UUID getId();

  /**
   * Gets the {@code content} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsComment}
   */
  Optional<String> getContent();

  /**
   * Gets the {@code author} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsComment}
   */
  Optional<User> getAuthor();

  /**
   * Gets the {@code post} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsComment}
   */
  Optional<Post> getPost();

  /**
   * Gets the {@code created_at} property, available on {@linkplain com.edgedb.examples.codegen.generated.results.GetUserCommentsComment}
   */
  NullableOptional<@Nullable OffsetDateTime> getCreatedAt();
}