package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserPostsResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserPosts.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:02.097764+02:00}<br/>
 * Edgeql hash: {@code c1152ca18ba877b8d5a5ef24fb8d094ae927a91edb18474c7425c8fd794db636}
 * @see GetUserPostsResult
 */
public final class GetUserPosts {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "SELECT Post {\n"
      + "    title,\n"
      + "    author: {\n"
      + "        name,\n"
      + "        joined_at\n"
      + "    },\n"
      + "    content,\n"
      + "    created_at\n"
      + "}\n"
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
   * The result of the query is represented as the generated class {@linkplain GetUserPostsResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserPostsResult}.
   */
  public static CompletionStage<List<GetUserPostsResult>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserPostsResult.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
