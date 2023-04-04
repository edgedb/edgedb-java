CREATE MIGRATION m1vxu37wczr357ppyrbhfon2msem5oczk7mjszhx2xzp2qlxpazana
    ONTO m1g554eizxm5losphzvy22vpfgzxwg6ecaxsq6hl7d7wuhfkk6qdtq
{
  CREATE GLOBAL examples::current_user_id -> std::uuid;
};
