CREATE MIGRATION m1ctpxq3sg4ovhtagu3shnidxgtxfab4qtffypldav7y7atracsh2q
    ONTO m172byr5jtuk2om22szly2keb2llil3ib4tfpz6fbnrfhoejvzfceq
{
  ALTER TYPE examples::Media {
      ALTER PROPERTY title {
          CREATE CONSTRAINT std::exclusive;
      };
  };
};
