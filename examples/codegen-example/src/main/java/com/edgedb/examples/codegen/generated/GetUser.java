package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserUser;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUser.edgeql}.<br/>
 * Generated on: {@code 2023-11-15T12:47:47.264427-05:00}<br/>
 * Edgeql hash: {@code ec6fd763528c072fa2801a23b62d176f426681c46b333384b98130e775494c1d}
 * @see GetUserUser
 */
public final class GetUser {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "SELECT User {\n"
      + "    id,\n"
      + "    name,\n"
      + "    joined_at,\n"
      + "    liked_posts: {\n"
      + "        id,\n"
      + "        title,\n"
      + "        created_at,\n"
      + "    },\n"
      + "    posts := (\n"
      + "        SELECT Post {\n"
      + "            id,\n"
      + "            title,\n"
      + "            created_at,\n"
      + "        } FILTER .author = User\n"
      + "    ),\n"
      + "    comments := (\n"
      + "        SELECT Comment {\n"
      + "            id,\n"
      + "            content,\n"
      + "            post: { id },\n"
      + "            created_at,\n"
      + "        } FILTER .author = User\n"
      + "    )\n"
      + "}\n"
      + "FILTER .name = <str>$name";

  /**
   * Executes the query defined in the file {@code GetUser.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *       module codegen
   *   SELECT User {
   *       id,
   *       name,
   *       joined_at,
   *       liked_posts: {
   *           id,
   *           title,
   *           created_at,
   *       },
   *       posts := (
   *           SELECT Post {
   *               id,
   *               title,
   *               created_at,
   *           } FILTER .author = User
   *       ),
   *       comments := (
   *           SELECT Comment {
   *               id,
   *               content,
   *               post: { id },
   *               created_at,
   *           } FILTER .author = User
   *       )
   *   }
   *   FILTER .name = <str>$name}</pre>
   * The result of the query is represented as the generated class {@linkplain GetUserUser}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserUser}.
   */
  public static CompletionStage<@Nullable GetUserUser> run(EdgeDBQueryable client, String name) {
      return client.querySingle(
          GetUserUser.class, 
          QUERY, 
          new HashMap<>(){{
            put("name", name);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY
          )
        );
  }
}
