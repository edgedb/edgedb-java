package com.edgedb.examples
import com.edgedb.driver.EdgeDBClient
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*
class BasicQueryFunctions extends Example:
  private val logger = LoggerFactory.getLogger(classOf[BasicQueryFunctions])

  override def run(client: EdgeDBClient)(implicit context: ExecutionContext): Future[Unit] = {
    for {
      queryResult <- client.query(classOf[String], "SELECT 'Hello, Scala!'").asScala
      querySingleResult <- client.querySingle(classOf[String], "SELECT 'Hello, Scala!'").asScala
      queryRequiredSingleResult <- client.queryRequiredSingle(classOf[String], "SELECT 'Hello, Scala!'").asScala
    }
    yield (queryResult, querySingleResult, queryRequiredSingleResult) {
      logger.info("'query' method result: {}", queryResult)
      logger.info("'querySingle' method result: {}", querySingleResult)
      logger.info("'queryRequiredSingle' method result: {}", queryRequiredSingleResult)
      0
    }
  }