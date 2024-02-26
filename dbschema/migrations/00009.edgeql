CREATE MIGRATION m1hds33s7w5uj53nhqljjf7cqpzzv64csyo7r4di4cf5f22q5ydywa
    ONTO m1xt33ahn2zm5wiwbdubmpttdcg5ywahjme2dnpj3jx3hfhkzcc3sq
{
  ALTER TYPE tests::Links {
      ALTER PROPERTY a {
          CREATE CONSTRAINT std::exclusive;
      };
  };
};
