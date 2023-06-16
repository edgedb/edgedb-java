package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient

import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.FutureConverters.FutureOps

class ScalaMain() {
  def runExamples(client: EdgeDBClient): CompletionStage[Unit] = Future {
    val examples = List(
      new ScalaAbstractTypes()
    ).map(_.run(client))

    Await.ready(Future.sequence(examples), Duration.Inf)
    ()
  }.asJava
}
