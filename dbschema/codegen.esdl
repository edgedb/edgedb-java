module codegen {
    global current_user_id: uuid;
    
    type User {
        required name: str {
            constraint exclusive;
            readonly := true;
        }

        joined_at: datetime {
            default := datetime_current();
            readonly := true;
        }

        multi liked_posts: Post;
    }

    type Post {
        required title: str;

        required author: User {
            readonly := true;
        }

        required content: str;

        created_at: datetime {
            default := datetime_current();
            readonly := true;
        }
    }

    type Comment {
        required author: User {
            readonly := true;
        }
        required post: Post {
            readonly := true;
        }

        required content: str;

        created_at: datetime {
            default := datetime_current();
            readonly := true;
        }
    }
}