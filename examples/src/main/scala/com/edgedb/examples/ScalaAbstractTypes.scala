package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}

class ScalaAbstractTypes() extends ScalaExample:
  private val logger = LoggerFactory.getLogger(classOf[ScalaAbstractTypes])

  import scala.jdk.FutureConverters._
  abstract class Media():
    var title: String = _

  class Movie() extends Media:
    var releaseYear: Long = _

  class Show() extends Media:
    var seasons: Long = _

  override def run(client: EdgeDBClient): Future[Unit] = {
    val results = client.execute(
      """insert Movie {
             title := "The Matrix",
             release_year := 1999
         } unless conflict on .title;
         insert Show {
             title := "The Office",
             seasons := 9
         } unless conflict on .title
         """.stripMargin
    ).asScala.flatMap { * =>
      client.query(
        classOf[Media],
        """select Media {
              title,
              [is Movie].release_year,
              [is Show].seasons
           }"""
      ).asScala
    }(ExecutionContext.global)

    results.map {
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
    }(ExecutionContext.global)
  }


