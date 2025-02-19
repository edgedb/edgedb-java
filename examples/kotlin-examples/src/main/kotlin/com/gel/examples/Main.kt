package com.gel.examples

import com.gel.driver.GelClientPool
import com.gel.driver.GelClientConfig
import com.gel.driver.GelConnection
import com.gel.driver.namingstrategies.NamingStrategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val clientPool = GelClientPool(GelClientConfig.builder()
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
                    example.runAsync(clientPool)
                    logger.info("Kotlin example {} complete!", example)
                } catch (x: Exception) {
                    logger.error("Failed to run Kotlin example {}", example, x)
                }
            }
        }

        exitProcess(0)
    }
}