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
 * Generated on: {@code 2023-11-15T12:47:47.477922-05:00}<br/>
 * Edgeql hash: {@code 03a47d0f0f8f786c98ad55318abaff22d9aa4d58fce167191d427bd6a02ba4d0}
 * @see CreateCommentComment
 */
public final class CreateComment {
  public static final String QUERY = "WITH\n"
      + "    module codegen,\n"
      + "    post_id := <uuid>$post_id\n"
      + "INSERT Comment {\n"
      + "    author := <User><uuid>$author_id,\n"
      + "    post := <Post>post_id,\n"
      + "    content := <str>$content\n"
      + "}";

  /**
   * Executes the query defined in the file {@code CreateComment.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH
   *       module codegen,
   *       post_id := <uuid>$post_id
   *   INSERT Comment {
   *       author := <User><uuid>$author_id,
   *       post := <Post>post_id,
   *       content := <str>$content
   *   }}</pre>
   * The result of the query is represented as the generated class {@linkplain CreateCommentComment}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreateCommentComment}.
   */
  public static CompletionStage<CreateCommentComment> run(EdgeDBQueryable client, UUID postId,
      UUID authorId, String content) {
      return client.queryRequiredSingle(
          CreateCommentComment.class, 
          QUERY, 
          new HashMap<>(){{
            put("post_id", postId);
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
