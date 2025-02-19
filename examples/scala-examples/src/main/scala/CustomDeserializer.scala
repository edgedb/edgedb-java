package com.gel.examples
import com.gel.driver.GelClientPool
import com.gel.driver.annotations.{GelDeserializer, GelName, GelType}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*

object CustomDeserializer:
  private val logger = LoggerFactory.getLogger(classOf[CustomDeserializer])

  @GelType
  class Person @GelDeserializer()
  (
    @GelName("name")
    val name: String,
    @GelName("age")
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