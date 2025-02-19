package com.gel.examples
import com.gel.driver.{GelClientPool, Transaction}
import org.slf4j.LoggerFactory
import scala.jdk.FutureConverters.*

import scala.concurrent.{ExecutionContext, Future}

class Transactions extends Example {
  private val logger = LoggerFactory.getLogger(classOf[Transactions])
  override def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit] = {
    // verify we can run transactions
    if (!client.supportsTransactions()) {
      logger.info("Skipping transactions, client type {} doesn't support it", client.getClientType)
      return Future.unit
    }

    client.transaction((tx: Transaction) => {
      logger.info("In transaction")
      tx.queryRequiredSingle(classOf[String], "select 'Result from Transaction'")
    }).asScala.map{ result =>
      logger.info("Result from transaction: {}", result)
    }
  }
}
