package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserLikedPostsPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserLikedPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T14:10:27.877227300-04:00}<br/>
 * Edgeql hash: {@code 1f3497b68a32121bb11aa23cb509dee9d3e0365e749abbddd79d69f86d41c9f3}
 * @see GetUserLikedPostsPost
 */
public final class GetUserLikedPosts {
  public static final String QUERY = "WITH \r\n"
      + "    module codegen,\r\n"
      + "    user := <User><uuid>$author_id\r\n"
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
   *     user := <User><uuid>$author_id
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
  public static CompletionStage<List<GetUserLikedPostsPost>> run(EdgeDBQueryable client,
      UUID authorId) {
      return client.query(
          GetUserLikedPostsPost.class, 
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
