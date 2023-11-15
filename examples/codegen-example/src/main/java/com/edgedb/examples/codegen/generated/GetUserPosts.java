package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserPostsPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUserPosts.edgeql}.<br/>
 * Generated on: {@code 2023-11-15T12:47:47.683057-05:00}<br/>
 * Edgeql hash: {@code f77ad16675a4b3605965ba85381b3b5a976ecc9f6cc72f5d1546f4dc97b8c69f}
 * @see GetUserPostsPost
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
