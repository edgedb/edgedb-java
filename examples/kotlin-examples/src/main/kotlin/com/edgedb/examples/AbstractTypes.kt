package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import com.edgedb.driver.annotations.EdgeDBType
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class AbstractTypes : Example {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTypes::class.java)!!
    }

    @EdgeDBType
    abstract class Media {
        var title: String? = null
    }

    @EdgeDBType
    class Show : Media() {
        var seasons: Long? = null
    }

    @EdgeDBType
    class Movie : Media() {
        var releaseYear: Long? = null
    }

    override suspend fun runAsync(client: EdgeDBClient) {
        client.execute(
                """insert Movie { 
                   title := "The Matrix", 
                   release_year := 1999 
               } unless conflict on .title;
               insert Show { 
                   title := "The Office", 
                   seasons := 9 
               } unless conflict on .title"""
        ).await()

        val results = client.query(
                Media::class.java,
                """select Media { 
                   title, 
                   [is Movie].release_year, 
                   [is Show].seasons 
               }"""
        ).await()

        for(result in results) {
            if(result is Show) {
                logger.info(
                        "Got show: title: {}, seasons: {}",
                        result.title, result.seasons
                )
            } else if(result is Movie) {
                logger.info(
                        "Got movie: title: {}, release year: {}",
                        result.title, result.releaseYear
                )
            }
        }
    }
}