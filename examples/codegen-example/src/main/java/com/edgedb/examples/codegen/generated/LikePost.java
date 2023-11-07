package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.LikePostUser;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

/**
 * A class containing the generated code responsible for the edgeql file {@code LikePost.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.755184500-04:00}<br/>
 * Edgeql hash: {@code 261d0a7da0e580a902d6b623428f9ea78c7dea0f91c7ac2ee0afe3885663c58a}
 * @see LikePostUser
 */
public final class LikePost {
  public static final String QUERY = "WITH \r\n"
      + "    module codegen,\r\n"
      + "    current_user := <User>global current_user_id,\r\n"
      + "    post_id := <uuid>$post_id\r\n"
      + "UPDATE current_user\r\n"
      + "SET {\r\n"
      + "    liked_posts += <Post>post_id\r\n"
      + "}";

  /**
   * Executes the query defined in the file {@code LikePost.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH 
   *     module codegen,
   *     current_user := <User>global current_user_id,
   *     post_id := <uuid>$post_id
   * UPDATE current_user
   * SET {
   *     liked_posts += <Post>post_id
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain LikePostUser}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain LikePostUser}.
   */
  public static CompletionStage<@Nullable LikePostUser> run(EdgeDBQueryable client, UUID postId) {
      return client.querySingle(
          LikePostUser.class, 
          QUERY, 
          new HashMap<>(){{
            put("post_id", postId);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
