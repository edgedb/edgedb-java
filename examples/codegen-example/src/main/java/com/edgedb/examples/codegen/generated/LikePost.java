package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.LikePostUser;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code LikePost.edgeql}.<br/>
 * Generated on: {@code 2023-11-14T11:16:52.941789400-04:00}<br/>
 * Edgeql hash: {@code 28ea11f3c3059ac7c43e1458c567d3328b1e39e20a3e170a9f3dbaeaa2ce10b6}
 * @see LikePostUser
 */
public final class LikePost {
  public static final String QUERY = "WITH \r\n"
      + "    module codegen,\r\n"
      + "    current_user := <User><uuid>$author_id,\r\n"
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
   *       module codegen,
   *       current_user := <User><uuid>$author_id,
   *       post_id := <uuid>$post_id
   *   UPDATE current_user
   *   SET {
   *       liked_posts += <Post>post_id
   *   }}</pre>
   * The result of the query is represented as the generated class {@linkplain LikePostUser}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain LikePostUser}.
   */
  public static CompletionStage<LikePostUser> run(EdgeDBQueryable client, UUID postId,
      UUID authorId) {
      return client.queryRequiredSingle(
          LikePostUser.class, 
          QUERY, 
          new HashMap<>(){{
            put("post_id", postId);
            put("author_id", authorId);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
