package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserLikedPostsResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserLikedPosts.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:02.013084+02:00}<br/>
 * Edgeql hash: {@code b47be463ea32deed8e6e90b45469d19c890db907a155b4e9f8522576f88345f6}
 * @see GetUserLikedPostsResult
 */
public final class GetUserLikedPosts {
  public static final String QUERY = "WITH \n"
      + "    module codegen,\n"
      + "    user := <User>global current_user_id\n"
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
   *     module codegen,
   *     user := <User>global current_user_id
   * SELECT user.liked_posts {
   *     title,
   *     content,
   *     author: {
   *         name,
   *         joined_at
   *     },
   *     created_at
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserLikedPostsResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserLikedPostsResult}.
   */
  public static CompletionStage<List<GetUserLikedPostsResult>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserLikedPostsResult.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
