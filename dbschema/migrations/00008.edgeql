CREATE MIGRATION m1cc3ijfa2zqq3cnvtypbvlnadp7qdespzfcpbldszk3fi3l2i34qq
    ONTO m1tig3qk3mnrb2xpszwyodgurkdeyza6yt67zo7kfljc2icy3e7yma
{
  CREATE MODULE codegen IF NOT EXISTS;
  CREATE TYPE codegen::Comment {
      CREATE PROPERTY content: std::str;
      CREATE PROPERTY created_at: std::datetime;
  };
  CREATE TYPE codegen::User {
      CREATE PROPERTY joined_at: std::datetime;
      CREATE PROPERTY name: std::str;
  };
  ALTER TYPE codegen::Comment {
      CREATE LINK author: codegen::User;
  };
  CREATE TYPE codegen::Post {
      CREATE LINK author: codegen::User;
      CREATE PROPERTY content: std::str;
      CREATE PROPERTY created_at: std::datetime;
  };
  ALTER TYPE codegen::Comment {
      CREATE LINK post: codegen::Post;
  };
  ALTER TYPE codegen::User {
      CREATE MULTI LINK liked_posts: codegen::Post;
  };
};
