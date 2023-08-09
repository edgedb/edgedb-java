package com.edgedb.examples.codegen;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.examples.codegen.generated.CreatePost;
import com.edgedb.examples.codegen.generated.CreateUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws EdgeDBException, IOException, ExecutionException, InterruptedException {
        var client = new EdgeDBClient()
                .withModule("codegen");

        CreateUser.run(client, "example_username")
                .thenApply(user -> client.withGlobals(new HashMap<>(){{
                    put("current_user_id", user.getId());
                }}))
                .thenCompose(userClient ->
                        CreatePost.run(userClient, "Hello World!", "This is my first post!")
                )
                .toCompletableFuture().get();
    }
}