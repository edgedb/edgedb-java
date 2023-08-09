package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.GetUserResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import org.jetbrains.annotations.Nullable;

/**
 * A class containing the generated code responsible for the edgeql file {@code GetUser.edgeql}.<br/>
 * Generated on: {@code 2023-08-09T20:38:01.497316+02:00}<br/>
 * Edgeql hash: {@code ef0047b01dd10d3a24fdf55887a9e4354745056e9c3df66992363a743f7ad68c}
 * @see GetUserResult
 */
public final class GetUser {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "SELECT User {\n"
      + "    id,\n"
      + "    name,\n"
      + "    joined_at,\n"
      + "}\n"
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
   * The result of the query is represented as the generated class {@linkplain GetUserResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain GetUserResult}.
   */
  public static CompletionStage<@Nullable GetUserResult> run(EdgeDBQueryable client, String name) {
      return client.querySingle(
          GetUserResult.class, 
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
