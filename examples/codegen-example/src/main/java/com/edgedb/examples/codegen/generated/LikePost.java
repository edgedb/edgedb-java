package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.LikePostResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

/**
 * A class containing the generated code responsible for the edgeql file {@code LikePost.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:01.666086+02:00}<br/>
 * Edgeql hash: {@code 0fd2cc5596e07c2a7a8fedc11d6ed224da031d5d85f153b14f3d092639e9e481}
 * @see LikePostResult
 */
public final class LikePost {
  public static final String QUERY = "WITH \n"
      + "    module codegen,\n"
      + "    current_user := <User>global current_user_id,\n"
      + "    post_id := <uuid>$post_id\n"
      + "UPDATE current_user\n"
      + "SET {\n"
      + "    liked_posts += <Post>post_id\n"
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
   * The result of the query is represented as the generated class {@linkplain LikePostResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain LikePostResult}.
   */
  public static CompletionStage<@Nullable LikePostResult> run(EdgeDBQueryable client, UUID postId) {
      return client.querySingle(
          LikePostResult.class, 
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
