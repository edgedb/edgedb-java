WITH
    module codegen
SELECT Post {
    title,
    author: {
        name,
        joined_at
    },
    content,
    created_at
}
FILTER .author.id = global current_user_id