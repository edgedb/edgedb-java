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
 * Generated on: {@code 2023-11-15T12:47:47.382881-05:00}<br/>
 * Edgeql hash: {@code 14b91fa021d16c11322dfce0cda923611ae1a454bcf3e757ec10914b8e3c0b11}
 * @see LikePostUser
 */
public final class LikePost {
  public static final String QUERY = "WITH \n"
      + "    module codegen,\n"
      + "    current_user := <User><uuid>$author_id,\n"
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
