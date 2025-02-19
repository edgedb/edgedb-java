package com.gel.examples

import scala.concurrent.{ExecutionContext, Future}
import com.gel.driver.GelClientPool

trait Example {
  def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit];
}