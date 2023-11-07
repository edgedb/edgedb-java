WITH
    module codegen,
    post_id := <uuid>$post_id
INSERT Comment {
    author := <User><uuid>$author_id,
    post := <Post>post_id,
    content := <str>$content
}