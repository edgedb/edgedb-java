module examples {
    type Person {
        required property name -> str;
        required property age -> int64;
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