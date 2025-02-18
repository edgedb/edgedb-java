package com.edgedb.examples

import com.edgedb.driver.GelClientPool
import com.edgedb.driver.annotations.GelType
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class AbstractTypes : Example {
    companion object {
        private val logger = LoggerFactory.getLogger(AbstractTypes::class.java)!!
    }

    @GelType
    abstract class Media {
        var title: String? = null
    }

    @GelType
    class Show : Media() {
        var seasons: Long? = null
    }

    @GelType
    class Movie : Media() {
        var releaseYear: Long? = null
    }

    override suspend fun runAsync(clientPool: GelClientPool) {
        clientPool.execute(
                """insert Movie { 
                   title := "The Matrix", 
                   release_year := 1999 
               } unless conflict on .title;
               insert Show { 
                   title := "The Office", 
                   seasons := 9 
               } unless conflict on .title"""
        ).await()

        val results = clientPool.query(
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