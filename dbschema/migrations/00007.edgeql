CREATE MIGRATION m1tig3qk3mnrb2xpszwyodgurkdeyza6yt67zo7kfljc2icy3e7yma
    ONTO m1vxu37wczr357ppyrbhfon2msem5oczk7mjszhx2xzp2qlxpazana
{
  CREATE MODULE tests IF NOT EXISTS;
  CREATE TYPE tests::TestDatastructure {
      CREATE REQUIRED PROPERTY a: std::str;
      CREATE REQUIRED PROPERTY b: std::str;
      CREATE REQUIRED PROPERTY c: std::str;
  };
};
