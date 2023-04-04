CREATE MIGRATION m1g554eizxm5losphzvy22vpfgzxwg6ecaxsq6hl7d7wuhfkk6qdtq
    ONTO m1cu7eeuryu52w6lllktnpaxxdffel426rxt7ejswhbfadw2mauvxq
{
  ALTER TYPE examples::Person {
      CREATE SINGLE LINK best_friend -> examples::Person;
      CREATE MULTI LINK friends -> examples::Person;
  };
};
