package com.edgedb.examples

import com.edgedb.driver.GelClientPool
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class GlobalsAndConfig : Example {
    companion object {
        private val logger = LoggerFactory.getLogger(GlobalsAndConfig::class.java)!!
    }

    override suspend fun runAsync(clientPool: GelClientPool) {
        val configuredClient = client
                .withConfig { config -> config
                        .withIdleTransactionTimeout(Duration.ZERO)
                        .applyAccessPolicies(true)
                }
                .withGlobals(mapOf(
                        "current_user_id" to UUID.randomUUID()
                ))

        val currentUserId = configuredClient.queryRequiredSingle(
                UUID::class.java,
                "SELECT GLOBAL current_user_id"
        ).await()

        logger.info("Current user ID: {}", currentUserId)
    }
}