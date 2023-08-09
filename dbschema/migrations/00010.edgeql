CREATE MIGRATION m1lvkwi56fx25bnhgarepdfnywpzlm725p6v2ln25kdc2e3zxwuyyq
    ONTO m1hiistex76qe4mq4xayvstblsisyxh7rlcuqofg2aen3s6nkph33q
{
  ALTER TYPE codegen::Post {
      CREATE REQUIRED PROPERTY title: std::str {
          SET REQUIRED USING (<std::str>{});
      };
  };
};
