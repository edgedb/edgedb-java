package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserPostsPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.730186100-04:00}<br/>
 * Edgeql hash: {@code 6d9e06460f2f96994306f419da29c27e3c00323e70215d3346936e85a7320751}
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
      + "FILTER .author.id = global current_user_id";

  /**
   * Executes the query defined in the file {@code GetUserPosts.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen
   * SELECT Post {
   *     title,
   *     author: {
   *         name,
   *         joined_at
   *     },
   *     content,
   *     created_at
   * }
   * FILTER .author.id = global current_user_id}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserPostsPost}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserPostsPost}.
   */
  public static CompletionStage<List<GetUserPostsPost>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserPostsPost.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
