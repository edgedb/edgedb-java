package com.edgedb.examples

import scala.concurrent.{ExecutionContext, Future}
import com.edgedb.driver.GelClientPool

trait Example {
  def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit];
}