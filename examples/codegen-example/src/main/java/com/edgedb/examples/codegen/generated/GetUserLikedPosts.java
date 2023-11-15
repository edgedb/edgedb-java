package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserLikedPostsResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserLikedPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-15T12:44:37.846400-05:00}<br/>
 * Edgeql hash: {@code a02acd65fd33ac6df74733a7b33b8d69f7ca071069d0601cc9fa2fda27930170}
 * @see GetUserLikedPostsResult
 */
public final class GetUserLikedPosts {
  public static final String QUERY = "WITH \n"
      + "    module codegen,\n"
      + "    user := <User><uuid>$author_id\n"
      + "SELECT user.liked_posts {\n"
      + "    title,\n"
      + "    content,\n"
      + "    author: {\n"
      + "        name,\n"
      + "        joined_at\n"
      + "    },\n"
      + "    created_at\n"
      + "}";

  /**
   * Executes the query defined in the file {@code GetUserLikedPosts.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH 
   *       module codegen,
   *       user := <User><uuid>$author_id
   *   SELECT user.liked_posts {
   *       title,
   *       content,
   *       author: {
   *           name,
   *           joined_at
   *       },
   *       created_at
   *   }}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserLikedPostsResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserLikedPostsResult}.
   */
  public static CompletionStage<List<GetUserLikedPostsResult>> run(EdgeDBQueryable client,
      UUID authorId) {
      return client.query(
          GetUserLikedPostsResult.class, 
          QUERY, 
          new HashMap<>(){{
            put("author_id", authorId);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
