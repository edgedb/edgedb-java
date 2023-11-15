WITH
    module codegen
SELECT User {
    id,
    name,
    joined_at,
    liked_posts: {
        id,
        title,
        created_at,
    },
    posts := (
        SELECT Post {
            id,
            title,
            created_at,
        } FILTER .author = User
    ),
    comments := (
        SELECT Comment {
            id,
            content,
            post: { id },
            created_at,
        } FILTER .author = User
    )
}
FILTER .name = <str>$name