module examples {
    global current_user_id -> uuid;
    
    type Person {
        required property name -> str {
            constraint exclusive;
        }

        required property age -> int64;

        multi link friends -> Person;
        single link best_friend -> Person;
    }

    abstract type Media {
        required property title -> str {
            constraint exclusive;
        }
    }

    type Movie extending Media {
        required property release_year -> int64;
    }

    type Show extending Media {
        required property seasons -> int64;
    }
}