package com.edgedb.examples
import com.edgedb.driver.GelClientPool
import com.edgedb.driver.annotations.{EdgeDBLinkType, EdgeDBType}
import org.slf4j.LoggerFactory

import java.util
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.*

object LinkProperties:
  @EdgeDBType
  class Person:
    var name: String = _
    var age: Long = _
    var bestFriend: Person = _

    @EdgeDBLinkType(classOf[Person])
    var friends: util.Collection[Person] = _

class LinkProperties extends Example:
  private val logger = LoggerFactory.getLogger(classOf[LinkProperties])
  import LinkProperties.Person

  override def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit] = {
    for(
      result <- client.queryRequiredSingle(
        classOf[Person],
        """
          | with
          |     a := (insert Person { name := 'Person A', age := 20 } unless conflict on .name),
          |     b := (insert Person { name := 'Person B', age := 21 } unless conflict on .name),
          |     c := (insert Person { name := 'Person C', age := 22, friends := b } unless conflict on .name)
          | insert Person {
          |     name := 'Person D',
          |     age := 23,
          |     friends := {
          |         a,
          |         b,
          |         c
          |     },
          |     best_friend := c
          | } unless conflict on .name;
          | select Person {
          |     name,
          |     age,
          |     friends: {
          |         name,
          |         age,
          |         friends
          |     },
          |     best_friend: {
          |         name,
          |         age,
          |         friends
          |     }
          | } filter .name = 'Person D'
          |""".stripMargin
      ).asScala
    ) yield {
      logger.info("Person with links: {}", result)
    }
  }