package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserCommentsResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserComments.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:01.364397+02:00}<br/>
 * Edgeql hash: {@code 4878e4272d2567d9ef69b3de18183e2a73e5c15c1d93fbe022efaaf01807f60e}
 * @see GetUserCommentsResult
 */
public final class GetUserComments {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "SELECT Comment {\n"
      + "    author: {\n"
      + "        name, \n"
      + "        joined_at\n"
      + "    },\n"
      + "    post: {\n"
      + "        title,\n"
      + "        content,\n"
      + "        author: {\n"
      + "            name,\n"
      + "            joined_at\n"
      + "        },\n"
      + "        created_at\n"
      + "    },\n"
      + "    content,\n"
      + "    created_at\n"
      + "}\n"
      + "FILTER .author.id = global current_user_id";

  /**
   * Executes the query defined in the file {@code GetUserComments.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen
   * SELECT Comment {
   *     author: {
   *         name, 
   *         joined_at
   *     },
   *     post: {
   *         title,
   *         content,
   *         author: {
   *             name,
   *             joined_at
   *         },
   *         created_at
   *     },
   *     content,
   *     created_at
   * }
   * FILTER .author.id = global current_user_id}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserCommentsResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserCommentsResult}.
   */
  public static CompletionStage<List<GetUserCommentsResult>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserCommentsResult.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
