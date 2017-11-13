package com.loyalty.theatre

import akka.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait StopActorSystemAfterAll extends BeforeAndAfterAll { this: (TestKit with Suite) =>
  override protected def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }
}
