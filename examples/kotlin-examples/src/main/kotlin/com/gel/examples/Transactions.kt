package com.gel.examples

import com.gel.driver.GelClientPool
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class Transactions : Example {
    companion object {
        private val logger = LoggerFactory.getLogger(Transactions::class.java)
    }

    override suspend fun runAsync(clientPool: GelClientPool) {
        // verify we can run transactions
        if (!clientPool.supportsTransactions()) {
            logger.info("Skipping transactions, client type {} doesn't support it", clientPool.clientType)
            return
        }

        val transactionResult = clientPool.transaction { tx ->
            tx.queryRequiredSingle(String::class.java, "SELECT 'Hello from transaction!'")
        }.await()

        logger.info("Transaction result: {}", transactionResult)
    }
}