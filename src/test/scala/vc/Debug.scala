package vc

import io.gatling.core.Predef._

import scala.concurrent.duration._

class Debug extends Simulation {

  setUp(
    CommonScenario().inject(
      atOnceUsers(1)
    ).protocols(createProtocol(Env.BASE_URL_80))
  )
    .maxDuration(2.minutes)
    .assertions(
      global.failedRequests.percent.lt(100)
    )
}