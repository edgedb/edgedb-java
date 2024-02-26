module tests {
    type TestDatastructure {
        required property a -> str;
        required property b -> str;
        required property c -> str;
    }

    type Links {
        a: str {
            constraint exclusive;
        };
        b: Links;
        multi c: Links;
    }
}