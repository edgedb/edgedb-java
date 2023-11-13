package com.edgedb.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.generated.results.GetUserPostsPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-13T15:56:05.765003600-04:00}<br/>
 * Edgeql hash: {@code bd5c7530ae22329782d013e73a141e65e5fb26b68d1f8b0ef40f32b67438cafd}
 * @see GetUserPostsPost
 */
public final class GetUserPosts {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "SELECT Post {\r\n"
      + "    title,\r\n"
      + "    author: {\r\n"
      + "        name,\r\n"
      + "        joined_at\r\n"
      + "    },\r\n"
      + "    content,\r\n"
      + "    created_at\r\n"
      + "}\r\n"
      + "FILTER .author.id = <uuid>$author_id";

  /**
   * Executes the query defined in the file {@code GetUserPosts.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *       module codegen
   *   SELECT Post {
   *       title,
   *       author: {
   *           name,
   *           joined_at
   *       },
   *       content,
   *       created_at
   *   }
   *   FILTER .author.id = <uuid>$author_id}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserPostsPost}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserPostsPost}.
   */
  public static CompletionStage<List<GetUserPostsPost>> run(EdgeDBQueryable client, UUID authorId) {
      return client.query(
          GetUserPostsPost.class, 
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
