package com.edgedb.examples;

import com.edgedb.driver.EdgeDBQueryable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public class AbstractTypes implements Example {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTypes.class);

    public static abstract class Media {
        public String title;
    }

    public static class Show extends Media {
        public Long seasons;
    }

    public static class Movie extends Media {
        public Long releaseYear;
    }

    @Override
    public CompletionStage<Void> run(EdgeDBQueryable client) {

        return client
                .execute("insert Movie { title := \"The Matrix\", release_year := 1999 } unless conflict on .title")
                .thenCompose(v -> client.execute("insert Show { title := \"The Office\", seasons := 9} unless conflict on .title"))
                .thenCompose(v -> client.query(Media.class, "select Media { title, [is Movie].release_year, [is Show].seasons }"))
                .thenAccept(content -> {
                    for (var media : content) {
                        if(media instanceof Show) {
                            logger.info("Got show {}", media);
                        }

                        if(media instanceof Movie) {
                            logger.info("Got movie {}", media);
                        }
                    }
                });
    }
}
