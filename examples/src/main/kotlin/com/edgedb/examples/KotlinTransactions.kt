package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class KotlinTransactions : KotlinExample {
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinTransactions::class.java)
    }

    override suspend fun runAsync(client: EdgeDBClient) {
        val transactionResult = client.transaction { tx ->
            tx.queryRequiredSingle(String::class.java, "SELECT 'Hello from transaction!'")
        }.await()

        logger.info("Transaction result: {}", transactionResult)
    }
}