CREATE MIGRATION m1xt33ahn2zm5wiwbdubmpttdcg5ywahjme2dnpj3jx3hfhkzcc3sq
    ONTO m1tig3qk3mnrb2xpszwyodgurkdeyza6yt67zo7kfljc2icy3e7yma
{
  CREATE TYPE tests::Links {
      CREATE LINK b: tests::Links;
      CREATE MULTI LINK c: tests::Links;
      CREATE PROPERTY a: std::str;
  };
};
