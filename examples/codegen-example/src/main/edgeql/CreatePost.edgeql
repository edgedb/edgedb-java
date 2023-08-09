WITH
    module codegen
INSERT Post {
    title := <str>$title,
    author := <User>global current_user_id,
    content := <str>$content
}