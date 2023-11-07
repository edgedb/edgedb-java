package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreateCommentComment;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreateComment.edgeql}.<br/>
 * Generated on: {@code 2023-11-07T11:00:36.545189400-04:00}<br/>
 * Edgeql hash: {@code c879137c703267dc5c1c75d139e49a733cd1209c3ecc681104048b1c9c5fdf28}
 * @see CreateCommentComment
 */
public final class CreateComment {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen,\r\n"
      + "    post_id := <uuid>$post_id\r\n"
      + "INSERT Comment {\r\n"
      + "    author := <User>global current_user_id,\r\n"
      + "    post := <Post>post_id,\r\n"
      + "    content := <str>$content\r\n"
      + "}";

  /**
   * Executes the query defined in the file {@code CreateComment.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen,
   *     post_id := <uuid>$post_id
   * INSERT Comment {
   *     author := <User>global current_user_id,
   *     post := <Post>post_id,
   *     content := <str>$content
   * }}</pre>
   * The result of the query is represented as the generated class {@linkplain CreateCommentComment}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreateCommentComment}.
   */
  public static CompletionStage<CreateCommentComment> run(EdgeDBQueryable client, UUID postId,
      String content) {
      return client.queryRequiredSingle(
          CreateCommentComment.class, 
          QUERY, 
          new HashMap<>(){{
            put("post_id", postId);
            put("content", content);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
