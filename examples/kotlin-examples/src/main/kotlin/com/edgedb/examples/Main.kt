package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import com.edgedb.driver.EdgeDBClientConfig
import com.edgedb.driver.namingstrategies.NamingStrategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val client = EdgeDBClient(EdgeDBClientConfig.builder()
                .withNamingStrategy(NamingStrategy.snakeCase())
                .useFieldSetters(true)
                .build()
        ).withModule("examples")



        val examples = listOf(
                AbstractTypes(),
                BasicQueries(),
                CustomDeserializer(),
                GlobalsAndConfig(),
                LinkProperties(),
                Transactions()
        )

        runBlocking {
            for (example in examples) {
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