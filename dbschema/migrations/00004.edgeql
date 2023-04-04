CREATE MIGRATION m1cu7eeuryu52w6lllktnpaxxdffel426rxt7ejswhbfadw2mauvxq
    ONTO m1ctpxq3sg4ovhtagu3shnidxgtxfab4qtffypldav7y7atracsh2q
{
  ALTER TYPE examples::Person {
      ALTER PROPERTY name {
          CREATE CONSTRAINT std::exclusive;
      };
  };
};
