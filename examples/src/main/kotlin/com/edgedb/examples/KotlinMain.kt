package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class KotlinMain {
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinMain::class.java)

        fun runExamples(client: EdgeDBClient) {
            val examples = listOf(
                    KotlinAbstractTypes(),
                    KotlinBasicQueries(),
                    KotlinCustomDeserializer(),
                    KotlinGlobalsAndConfig(),
                    KotlinLinkProperties(),
                    KotlinTransactions()
            )

            runBlocking {
                for(example in examples) {
                    logger.info("Running Kotlin example {}...", example)
                    try {
                        example.runAsync(client)
                        logger.info("Kotlin example {} complete!", example)
                    } catch (x: Exception) {
                        logger.error("Failed to run Kotlin example {}", example, x)
                    }
                }
            }
        }
    }
}