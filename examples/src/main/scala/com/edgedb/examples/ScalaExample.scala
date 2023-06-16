package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient

import scala.concurrent.Future

trait ScalaExample {
  def run(client: EdgeDBClient): Future[Unit];
}
