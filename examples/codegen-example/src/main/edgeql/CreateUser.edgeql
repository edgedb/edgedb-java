WITH
    module codegen
INSERT User {
    name := <str>$name
}
UNLESS CONFLICT ON .name ELSE (select User)