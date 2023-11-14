package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserCommentsComment;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserComments.edgeql}.<br/>
 * Generated on: {@code 2023-11-14T14:06:19.915004800-04:00}<br/>
 * Edgeql hash: {@code 8e8232921413c7354f7a4ee2e4441fee6f30c5bfbc0c2efee1e4a83f0a6d7fc8}
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
      + "FILTER .author.id = <uuid>$author_id";

  /**
   * Executes the query defined in the file {@code GetUserComments.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *       module codegen
   *   SELECT Comment {
   *       author: {
   *           name, 
   *           joined_at
   *       },
   *       post: {
   *           title,
   *           content,
   *           author: {
   *               name,
   *               joined_at
   *           },
   *           created_at
   *       },
   *       content,
   *       created_at
   *   }
   *   FILTER .author.id = <uuid>$author_id}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserCommentsComment}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserCommentsComment}.
   */
  public static CompletionStage<List<GetUserCommentsComment>> run(EdgeDBQueryable client,
      UUID authorId) {
      return client.query(
          GetUserCommentsComment.class, 
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
