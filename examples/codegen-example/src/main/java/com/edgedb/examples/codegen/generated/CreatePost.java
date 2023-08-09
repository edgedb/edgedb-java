package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreatePostResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreatePost.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:02.188019+02:00}<br/>
 * Edgeql hash: {@code 5317653bc588a6f08c02ca7874be3ccda700598749de05f3a42c6d0201978ca3}
 * @see CreatePostResult
 */
public final class CreatePost {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "INSERT Post {\n"
      + "    title := <str>$title,\n"
      + "    author := <User>global current_user_id,\n"
      + "    content := <str>$content\n"
      + "}";

  /**
   * Executes the query defined in the file {@code CreatePost.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen
   * INSERT Post {
   *     title := <str>$title,
   *     author := <User>global current_user_id,
   *     content := <str>$content
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain CreatePostResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreatePostResult}.
   */
  public static CompletionStage<CreatePostResult> run(EdgeDBQueryable client, String title,
      String content) {
      return client.queryRequiredSingle(
          CreatePostResult.class, 
          QUERY, 
          new HashMap<>(){{
            put("title", title);
            put("content", content);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
