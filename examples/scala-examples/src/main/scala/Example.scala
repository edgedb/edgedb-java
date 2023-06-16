package com.edgedb.examples

import scala.concurrent.{ExecutionContext, Future}
import com.edgedb.driver.EdgeDBClient

trait Example {
  def run(client: EdgeDBClient)(implicit context: ExecutionContext): Future[Unit];
}