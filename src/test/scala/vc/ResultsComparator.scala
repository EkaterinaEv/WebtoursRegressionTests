package vc

import java.io.File
import scala.io.Source
import scala.util.Using

object ResultsComparator {

  case class Metrics(
                      total: Int,
                      errors: Int,
                      errorPercent: Double,
                      mean: Double,
                      p95: Double,
                      p99: Double
                    )

  def parseLog(path: String): Metrics = {
    val lines = Using.resource(Source.fromFile(path)) {
      _.getLines().toList
    }

    val requests = lines.filter(_.contains("REQUEST"))
    val ok = requests.filter(_.contains("OK"))
    val ko = requests.filter(_.contains("KO"))

    val times = requests.flatMap { line =>
      """(\d+)ms""".r.findFirstMatchIn(line).map(_.group(1).toInt)
    }.sorted.map(_.toDouble)

    Metrics(
      total = requests.size,
      errors = ko.size,
      errorPercent = if (requests.nonEmpty) (ko.size.toDouble / requests.size) * 100 else 0.0,
      mean = if (times.nonEmpty) times.sum / times.size else 0.0,
      p95 = if (times.nonEmpty) times(((times.size * 0.95).toInt).min(times.size - 1)) else 0.0,
      p99 = if (times.nonEmpty) times(((times.size * 0.99).toInt).min(times.size - 1)) else 0.0
    )
  }

  def getLatestLog(prefix: String): Option[String] = {
    val dir = new File("gatling/results")
    if (!dir.exists()) return None

    dir.listFiles()
      .filter(_.isDirectory)
      .filter(_.getName.startsWith(prefix))
      .sortBy(_.getName)
      .reverse
      .headOption
      .map(f => s"${f.getAbsolutePath}/simulation.log")
  }

  def compare(): Unit = {
    val baseline = getLatestLog("baselinetest")
    val regression = getLatestLog("regressiontest")

    if (baseline.isEmpty || regression.isEmpty) {
      println("Logs not found. Run tests first.")
      return
    }

    val b = parseLog(baseline.get)
    val r = parseLog(regression.get)

    println("\nComparison Results:")
    println("=" * 60)
    println(f"Metric${" " * 20} Baseline  Regression  Change")
    println("-" * 60)

    def diff(base: Double, reg: Double): Double =
      if (base != 0) ((reg - base) / base) * 100 else 0.0

    println(f"Total requests${" " * 10} ${b.total}%8d  ${r.total}%10d  ${diff(b.total, r.total)}%+8.1f%%")
    println(f"Error rate${" " * 13} ${b.errorPercent}%7.2f%%  ${r.errorPercent}%9.2f%%  ${diff(b.errorPercent, r.errorPercent)}%+8.1f%%")
    println(f"Mean (ms)${" " * 15} ${b.mean}%8.0f  ${r.mean}%10.0f  ${diff(b.mean, r.mean)}%+8.1f%%")
    println(f"95th pct${" " * 15} ${b.p95}%8.0f  ${r.p95}%10.0f  ${diff(b.p95, r.p95)}%+8.1f%%")
    println(f"99th pct${" " * 15} ${b.p99}%8.0f  ${r.p99}%10.0f  ${diff(b.p99, r.p99)}%+8.1f%%")

    println("=" * 60)

    val meanDeg = diff(b.mean, r.mean)
    val errDeg = r.errorPercent - b.errorPercent

    if (meanDeg > 10 || errDeg > 1) {
      println("VERDICT: RELEASE NOT RECOMMENDED - CRITICAL DEGRADATION")
    } else if (meanDeg > 5 || errDeg > 0.5) {
      println("VERDICT: RELEASE WITH CAUTION - MINOR DEGRADATION")
    } else {
      println("VERDICT: RELEASE RECOMMENDED - NO DEGRADATION")
    }
    println("=" * 60)
  }

  def main(args: Array[String]): Unit = compare()
}