package vc

import io.gatling.core.Predef._

import scala.concurrent.duration._

class InjectionProfile extends Simulation {

  private val MAX_USERS = 100 // Максимум пользователей (100%)
  private val STEP_USERS = MAX_USERS / 10 // 10 пользователей = 10%
  private val STEP_DURATION = 5.minutes // Длительность каждой ступени

  private val scn = CommonScenario()

  setUp(
    scn.inject(
      // Разогрев
      rampConcurrentUsers(0).to(5).during(30.seconds),

      // 10 ступеней по 5 минут
      rampConcurrentUsers(0).to(STEP_USERS).during(30.seconds),
      constantConcurrentUsers(STEP_USERS).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 2).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 2).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 3).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 3).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 4).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 4).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 5).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 5).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 6).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 6).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 7).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 7).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 8).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 8).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 9).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 9).during(STEP_DURATION),

      rampConcurrentUsers(0).to(STEP_USERS * 10).during(30.seconds),
      constantConcurrentUsers(STEP_USERS * 10).during(STEP_DURATION)
    ).protocols(httpProtocol)
  )
    .maxDuration(STEP_DURATION * 10 + 10.minutes)
    .assertions(
      global.failedRequests.percent.lt(5),
      global.responseTime.mean.lt(3000),
      global.responseTime.percentile(95).lt(5000),
      global.responseTime.percentile(99).lt(8000)
    )
}