package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreatePostPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreatePost.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T14:10:27.659227900-04:00}<br/>
 * Edgeql hash: {@code 56ebb4f23fb0cdcc2882915a7ea173106b04f9263e793a4e9e0330e2a00420e9}
 * @see CreatePostPost
 */
public final class CreatePost {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "INSERT Post {\r\n"
      + "    title := <str>$title,\r\n"
      + "    author := <User><uuid>$author_id,\r\n"
      + "    content := <str>$content\r\n"
      + "}";

  /**
   * Executes the query defined in the file {@code CreatePost.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen
   * INSERT Post {
   *     title := <str>$title,
   *     author := <User><uuid>$author_id,
   *     content := <str>$content
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain CreatePostPost}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreatePostPost}.
   */
  public static CompletionStage<CreatePostPost> run(EdgeDBQueryable client, String title,
      UUID authorId, String content) {
      return client.queryRequiredSingle(
          CreatePostPost.class, 
          QUERY, 
          new HashMap<>(){{
            put("title", title);
            put("author_id", authorId);
            put("content", content);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
