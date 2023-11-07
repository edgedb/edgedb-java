package com.edgedb.examples.codegen;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.examples.codegen.generated.CreateComment;
import com.edgedb.examples.codegen.generated.CreatePost;
import com.edgedb.examples.codegen.generated.CreateUser;
import com.edgedb.examples.codegen.generated.LikePost;
import com.edgedb.examples.codegen.generated.interfaces.Post;
import com.edgedb.examples.codegen.generated.interfaces.User;
import com.edgedb.examples.codegen.generated.results.CreateCommentComment;
import com.edgedb.examples.codegen.generated.results.CreatePostPost;
import com.edgedb.examples.codegen.generated.results.LikePostUser;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws EdgeDBException, IOException, ExecutionException, InterruptedException {
        var client = new EdgeDBClient()
                .withModule("codegen");

        runGeneratedQueries(client).toCompletableFuture().get();
    }

    private static CompletionStage<LikePostUser> runGeneratedQueries(EdgeDBClient client) {
        return CreateUser.run(client, "username")
                .thenCompose(user ->
                    createPost(client, user, "My First Post", "This is a post!")
                )
                .thenCompose(post ->
                    createComment(client, post, post.getAuthor().orElseThrow(), "Wow! Epic post!")
                )
                .thenCompose(comment ->
                    LikePost.run(client, comment.getPost().orElseThrow().getId(), comment.getAuthor().orElseThrow().getId())
                );
    }

    private static CompletionStage<CreatePostPost> createPost(EdgeDBClient client, User author, String title, String content) {
        return CreatePost.run(client, title, author.getId(), content);
    }

    private static CompletionStage<CreateCommentComment> createComment(EdgeDBClient client, Post post, User author, String content) {
        return CreateComment.run(client, post.getId(), author.getId(), content);
    }
}