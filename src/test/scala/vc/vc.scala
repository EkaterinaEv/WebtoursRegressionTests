import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

package object vc {

  object Env {
    val BASE_URL_80 = "http://webtours.load-test.ru:1080"
    val BASE_URL_90 = "http://webtours.load-test.ru:1090"
  }

  def createProtocol(baseUrl: String): HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .proxy(Proxy("localhost", 8888))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36")
    .disableFollowRedirect

}