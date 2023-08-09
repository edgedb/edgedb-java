WITH 
    module codegen,
    user := <User>global current_user_id
SELECT user.liked_posts {
    title,
    content,
    author: {
        name,
        joined_at
    },
    created_at
}