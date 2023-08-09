WITH
    module codegen
SELECT User {
    id,
    name,
    joined_at,
}
FILTER .name = <str>$name