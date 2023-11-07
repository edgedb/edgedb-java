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
 * Generated on: {@code 2023-11-07T14:10:27.726227600-04:00}<br/>
 * Edgeql hash: {@code 9e084d7e0f3ffab82a66561d6221d0739b76453f485ec7ada58dca46f5251df8}
 * @see GetUserUser
 */
public final class GetUser {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "SELECT User {\r\n"
      + "    id,\r\n"
      + "    name,\r\n"
      + "    joined_at,\r\n"
      + "}\r\n"
      + "FILTER .name = <str>$name";

  /**
   * Executes the query defined in the file {@code GetUser.edgeql} with the capabilities {@code read only}.
   * The query:
   * <pre>
   * {@literal WITH
   *     module codegen
   * SELECT User {
   *     id,
   *     name,
   *     joined_at,
   * }
   * FILTER .name = <str>$name}</pre>
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
