package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserLikedPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.702184600-04:00}<br/>
 * Edgeql hash: {@code e6f1f76c71634fdd4c6e9b2bc8a9c86230015f711739d1a59e2f10d1be2d1339}
 * @see GetUserLikedPostsPost
 */
public final class GetUserLikedPosts {
  public static final String QUERY = "WITH \r\n"
      + "    module codegen,\r\n"
      + "    user := <User>global current_user_id\r\n"
      + "SELECT user.liked_posts {\r\n"
      + "    title,\r\n"
      + "    content,\r\n"
      + "    author: {\r\n"
      + "        name,\r\n"
      + "        joined_at\r\n"
      + "    },\r\n"
      + "    created_at\r\n"
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
   * The result of the query is represented as the generated class {@linkplain GetUserLikedPostsPost}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserLikedPostsPost}.
   */
  public static CompletionStage<List<GetUserLikedPostsPost>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserLikedPostsPost.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
