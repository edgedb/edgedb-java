WITH
    module codegen
INSERT Post {
    title := <str>$title,
    author := <User><uuid>$author_id,
    content := <str>$content
}