package com.edgedb.generated;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.EdgeDBQueryable;
import com.edgedb.generated.results.CreateUserUser;
import java.lang.String;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

/**
 * A class containing the generated code responsible for the edgeql file {@code CreateUser.edgeql}.<br/>
 * Generated on: {@code 2023-11-13T15:56:05.634003700-04:00}<br/>
 * Edgeql hash: {@code f36ffbe222a09889aac91c16372d6533427dcc4b6d1b8d5f5cf601b4e9564168}
 * @see CreateUserUser
 */
public final class CreateUser {
  public static final String QUERY = "WITH\r\n"
      + "    module codegen\r\n"
      + "INSERT User {\r\n"
      + "    name := <str>$name\r\n"
      + "}\r\n"
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
   * The result of the query is represented as the generated class {@linkplain CreateUserUser}
   * @return A {@linkplain CompletionStage} that represents the asynchronous operation of executing the query and 
   * parsing the result. The {@linkplain CompletionStage} result is {@linkplain CreateUserUser}.
   */
  public static CompletionStage<CreateUserUser> run(EdgeDBQueryable client, String name) {
      return client.queryRequiredSingle(
          CreateUserUser.class, 
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
