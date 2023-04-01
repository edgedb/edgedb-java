CREATE MIGRATION m172byr5jtuk2om22szly2keb2llil3ib4tfpz6fbnrfhoejvzfceq
    ONTO m1y4l2krj2xdgex6kza3wbpkbrxljaobmtxuofw6gyujg3qjpjvb2a
{
  CREATE MODULE examples IF NOT EXISTS;
  ALTER TYPE default::Person RENAME TO examples::Person;
  CREATE ABSTRACT TYPE examples::Media {
      CREATE REQUIRED PROPERTY title -> std::str;
  };
  CREATE TYPE examples::Movie EXTENDING examples::Media {
      CREATE REQUIRED PROPERTY release_year -> std::int64;
  };
  CREATE TYPE examples::Show EXTENDING examples::Media {
      CREATE REQUIRED PROPERTY seasons -> std::int64;
  };
};
