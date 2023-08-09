WITH 
    module codegen,
    current_user := <User>global current_user_id,
    post_id := <uuid>$post_id
UPDATE current_user
SET {
    liked_posts += <Post>post_id
}