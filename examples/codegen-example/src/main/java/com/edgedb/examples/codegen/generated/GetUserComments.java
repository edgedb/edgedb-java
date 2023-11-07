package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserCommentsComment;
import java.lang.String;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserComments.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.674183900-04:00}<br/>
 * Edgeql hash: {@code c1b622c6b73f72a6791ab7090931148ac0cd9fc077f59cff12a11270de6e1e00}
 * @see GetUserCommentsComment
 */
public final class GetUserComments {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "SELECT Comment {\r\n"
      + "    author: {\r\n"
      + "        name, \r\n"
      + "        joined_at\r\n"
      + "    },\r\n"
      + "    post: {\r\n"
      + "        title,\r\n"
      + "        content,\r\n"
      + "        author: {\r\n"
      + "            name,\r\n"
      + "            joined_at\r\n"
      + "        },\r\n"
      + "        created_at\r\n"
      + "    },\r\n"
      + "    content,\r\n"
      + "    created_at\r\n"
      + "}\r\n"
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
   * The result of the query is represented as the generated class {@linkplain GetUserCommentsComment}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserCommentsComment}.
   */
  public static CompletionStage<List<GetUserCommentsComment>> run(EdgeDBQueryable client) {
      return client.query(
          GetUserCommentsComment.class, 
          QUERY, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
