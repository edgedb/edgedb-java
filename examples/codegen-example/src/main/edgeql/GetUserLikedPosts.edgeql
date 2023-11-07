WITH 
    module codegen,
    user := <User><uuid>$author_id
SELECT user.liked_posts {
    title,
    content,
    author: {
        name,
        joined_at
    },
    created_at
}