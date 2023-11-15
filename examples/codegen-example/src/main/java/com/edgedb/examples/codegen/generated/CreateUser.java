package com.edgedb.examples.codegen.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.examples.codegen.generated.results.CreateUserResult;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreateUser.edgeql}.<br/>
 * Generated on: {@code 2023-11-15T12:44:37.791533-05:00}<br/>
 * Edgeql hash: {@code 4eaa50c4e96f883150fa9f9e2f32a59925a4d17610cb77ba9fda0b5eace9f4d6}
 * @see CreateUserResult
 */
public final class CreateUser {
  public static final String QUERY = "WITH\n"
      + "    module codegen\n"
      + "INSERT User {\n"
      + "    name := <str>$name\n"
      + "}\n"
      + "UNLESS CONFLICT ON .name ELSE (select User)";

  /**
   * Executes the query defined in the file {@code CreateUser.edgeql} with the capabilities {@code read only, modifications}.
   * The query:
   * <pre>
   * {@literal WITH
   *       module codegen
   *   INSERT User {
   *       name := <str>$name
   *   }
   *   UNLESS CONFLICT ON .name ELSE (select User)}</pre>
   * The result of the query is represented as the generated class {@linkplain CreateUserResult}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreateUserResult}.
   */
  public static CompletionStage<CreateUserResult> run(EdgeDBQueryable client, String name) {
      return client.queryRequiredSingle(
          CreateUserResult.class, 
          QUERY, 
          new HashMap<>(){{
            put("name", name);
          }}, 
          EnumSet.of(
            Capabilities.READ_ONLY,
            Capabilities.MODIFICATIONS
          )
        );
  }
}
