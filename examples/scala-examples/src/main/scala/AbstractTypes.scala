package com.edgedb.examples

import com.edgedb.driver.GelClientPool
import com.edgedb.driver.annotations.EdgeDBType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}

object AbstractTypes:
  @EdgeDBType
  abstract class Media:
    var title: String = _

  @EdgeDBType
  class Movie extends Media:
    var releaseYear: Long = _

  @EdgeDBType
  class Show extends Media:
    var seasons: Long = _

class AbstractTypes extends Example:
  private val logger = LoggerFactory.getLogger(classOf[AbstractTypes])

  import scala.jdk.FutureConverters._
  import AbstractTypes._

  override def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit] = {
    for {
      result <- client.query(
        classOf[Media],
        """
          | insert Movie {
          |     title := "The Matrix",
          |     release_year := 1999
          | } unless conflict on .title;
          | insert Show {
          |     title := "The Office",
          |     seasons := 9
          | } unless conflict on .title;
          | select Media {
          |     title,
          |     [is Movie].release_year,
          |     [is Show].seasons
          |}""".stripMargin).asScala
    } yield {
      result.forEach {
        case movie: Movie =>
          logger.info(
            "Got movie: title: {}, release year: {}",
            movie.title, movie.releaseYear
          )
        case show: Show =>
          logger.info(
            "Got show: title: {}, seasons: {}",
            show.title, show.seasons
          )
        case unknown => logger.warn("Got unknown result type: {}", unknown)
      }
    }
  }
