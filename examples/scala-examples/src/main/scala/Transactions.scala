package com.edgedb.examples
import com.edgedb.driver.{EdgeDBClient, Transaction}
import org.slf4j.LoggerFactory
import scala.jdk.FutureConverters.*

import scala.concurrent.{ExecutionContext, Future}

class Transactions extends Example {
  private val logger = LoggerFactory.getLogger(classOf[Transactions])
  override def run(client: EdgeDBClient)(implicit context: ExecutionContext): Future[Unit] = {
    client.transaction((tx: Transaction) => {
      logger.info("In transaction")
      tx.queryRequiredSingle(classOf[String], "select 'Result from Transaction'")
    }).asScala.map{ result =>
      logger.info("Result from transaction: {}", result)
    }
  }
}
