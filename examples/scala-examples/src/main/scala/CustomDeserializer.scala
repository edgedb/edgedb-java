package com.edgedb.examples
import com.edgedb.driver.GelClientPool
import com.edgedb.driver.annotations.{GelDeserializer, EdgeDBName, EdgeDBType}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*

object CustomDeserializer:
  private val logger = LoggerFactory.getLogger(classOf[CustomDeserializer])

  @EdgeDBType
  class Person @GelDeserializer()
  (
    @EdgeDBName("name")
    val name: String,
    @EdgeDBName("age")
    val age: Long
  ) {
    logger.info("Custom deserializer called")
  }

class CustomDeserializer extends Example:
  import CustomDeserializer._

  override def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit] = {
    for(
      result <- client.queryRequiredSingle(
        classOf[Person],
        """
          | insert Person { name := 'Example', age := 123 } unless conflict on .name;
          | select Person { name, age } filter .name = 'Example'
          |""".stripMargin
      ).asScala
    ) yield {
      logger.info("Got person: name: {}, age: {}", result.name, result.age)
    }
  }