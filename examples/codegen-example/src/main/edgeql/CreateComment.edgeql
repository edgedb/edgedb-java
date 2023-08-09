WITH
    module codegen,
    post_id := <uuid>$post_id
INSERT Comment {
    author := <User>global current_user_id,
    post := <Post>post_id,
    content := <str>$content
}