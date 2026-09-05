package vc

import io.gatling.core.Predef._

import scala.concurrent.duration._

class BaselineTest extends Simulation {

  private val TARGET_USERS = 80
  private val TEST_DURATION = 15.minutes

  private val scn = CommonScenario()

  setUp(
    scn.inject(
      rampConcurrentUsers(0).to(TARGET_USERS).during(5.minutes),
      constantConcurrentUsers(TARGET_USERS).during(TEST_DURATION)
    ).protocols(createProtocol(Env.BASE_URL_80))
  )
    .maxDuration(TEST_DURATION + 10.minutes)
    .assertions(
      global.failedRequests.percent.lt(1),
      global.responseTime.mean.lt(2000),
      global.responseTime.percentile(95).lt(4000)
    )
}