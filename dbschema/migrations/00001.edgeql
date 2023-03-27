CREATE MIGRATION m1y4l2krj2xdgex6kza3wbpkbrxljaobmtxuofw6gyujg3qjpjvb2a
    ONTO initial
{
  CREATE FUTURE nonrecursive_access_policies;
  CREATE TYPE default::Person {
      CREATE REQUIRED PROPERTY age -> std::int64;
      CREATE REQUIRED PROPERTY name -> std::str;
  };
};
