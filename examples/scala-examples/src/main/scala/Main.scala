package com.edgedb.examples

import com.edgedb.driver.namingstrategies.NamingStrategy
import com.edgedb.driver.{EdgeDBClient, EdgeDBClientConfig, Transaction}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.util.control.NonFatal
import ExecutionContext.Implicits.global

@main
def main(): Unit = {
  val logger = LoggerFactory.getLogger("Main")

  val client = new EdgeDBClient(EdgeDBClientConfig.builder
    .withNamingStrategy(NamingStrategy.snakeCase)
    .useFieldSetters(true)
    .build
  ).withModule("examples")

  val examples = List(
    AbstractTypes(),
    BasicQueryFunctions(),
    CustomDeserializer(),
    GlobalsAndConfig(),
    LinkProperties(),
    Transactions()
  )

  for (example <- examples)
    Await.ready(runExample(logger, client, example), Duration.Inf)

  logger.info("Examples complete!")

  System.exit(0)
}

private def runExample(logger: Logger, client: EdgeDBClient, example: Example)(implicit context: ExecutionContext): Future[Unit] = {
  logger.info("Running Scala example {}...", example)
  example.run(client)
    .map({ * =>
      logger.info(s"Scala example {} complete!", example)
    })
    .recoverWith { e =>
      logger.error(s"Failed to run Scala example {}", example, e)

      Future {}
    }
}