CREATE MIGRATION m1hiistex76qe4mq4xayvstblsisyxh7rlcuqofg2aen3s6nkph33q
    ONTO m1cc3ijfa2zqq3cnvtypbvlnadp7qdespzfcpbldszk3fi3l2i34qq
{
  ALTER TYPE codegen::Comment {
      ALTER LINK author {
          SET readonly := true;
          SET REQUIRED USING (<codegen::User>{});
      };
      ALTER LINK post {
          SET readonly := true;
          SET REQUIRED USING (<codegen::Post>{});
      };
      ALTER PROPERTY content {
          SET REQUIRED USING (<std::str>{});
      };
      ALTER PROPERTY created_at {
          SET default := (std::datetime_current());
          SET readonly := true;
      };
  };
  ALTER TYPE codegen::Post {
      ALTER LINK author {
          SET readonly := true;
          SET REQUIRED USING (<codegen::User>{});
      };
      ALTER PROPERTY content {
          SET REQUIRED USING (<std::str>{});
      };
      ALTER PROPERTY created_at {
          SET default := (std::datetime_current());
          SET readonly := true;
      };
  };
  ALTER TYPE codegen::User {
      ALTER PROPERTY joined_at {
          SET default := (std::datetime_current());
          SET readonly := true;
      };
      ALTER PROPERTY name {
          SET readonly := true;
          CREATE CONSTRAINT std::exclusive;
          SET REQUIRED USING (<std::str>{});
      };
  };
};
