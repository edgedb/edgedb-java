package com.edgedb.examples

import com.edgedb.driver.GelClientPool
import com.edgedb.driver.EdgeDBClientConfig
import com.edgedb.driver.GelConnection
import com.edgedb.driver.namingstrategies.NamingStrategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val clientPool = GelClientPool(EdgeDBClientConfig.builder()
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
                    example.runAsync(cclientPoollient)
                    logger.info("Kotlin example {} complete!", example)
                } catch (x: Exception) {
                    logger.error("Failed to run Kotlin example {}", example, x)
                }
            }
        }

        exitProcess(0)
    }
}