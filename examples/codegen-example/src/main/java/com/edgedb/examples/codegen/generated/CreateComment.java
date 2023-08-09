package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreateCommentResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreateComment.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:01.819630+02:00}<br/>
 * Edgeql hash: {@code 00ed6918dc3625ff1237aab89dbe1358bb401a74826bba5aebe7440949e24009}
 * @see CreateCommentResult
 */
public final class CreateComment {
  public static final String QUERY = "WITH\n"
      + "    module codegen,\n"
      + "    post_id := <uuid>$post_id\n"
      + "INSERT Comment {\n"
      + "    author := <User>global current_user_id,\n"
      + "    post := <Post>post_id,\n"
      + "    content := <str>$content\n"
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
   * The result of the query is represented as the generated class {@linkplain CreateCommentResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreateCommentResult}.
   */
  public static CompletionStage<CreateCommentResult> run(EdgeDBQueryable client, UUID postId,
      String content) {
      return client.queryRequiredSingle(
          CreateCommentResult.class, 
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
