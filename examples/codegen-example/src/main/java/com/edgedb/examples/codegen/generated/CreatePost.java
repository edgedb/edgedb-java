package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreatePostPost;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreatePost.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.580184100-04:00}<br/>
 * Edgeql hash: {@code e7c2761c4ad3282c3122aaae9549bc890aba0987e0c2e1e66470101379fe7941}
 * @see CreatePostPost
 */
public final class CreatePost {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "INSERT Post {\r\n"
      + "    title := <str>$title,\r\n"
      + "    author := <User>global current_user_id,\r\n"
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
   *     author := <User>global current_user_id,
   *     content := <str>$content
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain CreatePostPost}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreatePostPost}.
   */
  public static CompletionStage<CreatePostPost> run(EdgeDBQueryable client, String title,
      String content) {
      return client.queryRequiredSingle(
          CreatePostPost.class, 
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
