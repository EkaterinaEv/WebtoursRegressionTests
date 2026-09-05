package vc

import java.io.File
import scala.io.Source

object ResultsComparator {

  case class TestMetrics(
                          totalRequests: Int,
                          successfulRequests: Int,
                          failedRequests: Int,
                          errorPercent: Double,
                          meanResponseTime: Double,
                          p95ResponseTime: Double,
                          p99ResponseTime: Double,
                          minResponseTime: Double,
                          maxResponseTime: Double
                        )

  def parseSimulationLog(logPath: String): TestMetrics = {
    val logFile = new File(logPath)
    if (!logFile.exists()) {
      throw new Exception(s"Log file not found: $logPath")
    }

    val lines = Source.fromFile(logFile).getLines().toList
    val requestLines = lines.filter(_.contains("REQUEST"))
    val okLines = requestLines.filter(_.contains("OK"))
    val koLines = requestLines.filter(_.contains("KO"))

    val total = requestLines.size
    val ok = okLines.size
    val ko = koLines.size

    // Парсинг времени ответа
    val responseTimes = requestLines.flatMap { line =>
      // Пример: "REQUEST OK ... 234ms"
      val pattern = """(\d+)ms""".r
      pattern.findFirstMatchIn(line).map(_.group(1).toInt)
    }.map(_.toDouble)

    val sortedTimes = responseTimes.sorted
    val mean = if (sortedTimes.nonEmpty) sortedTimes.sum / sortedTimes.size else 0.0
    val p95Index = (sortedTimes.size * 0.95).toInt
    val p99Index = (sortedTimes.size * 0.99).toInt

    TestMetrics(
      totalRequests = total,
      successfulRequests = ok,
      failedRequests = ko,
      errorPercent = if (total > 0) (ko.toDouble / total) * 100 else 0.0,
      meanResponseTime = mean,
      p95ResponseTime = if (sortedTimes.nonEmpty) sortedTimes(p95Index) else 0.0,
      p99ResponseTime = if (sortedTimes.nonEmpty) sortedTimes(p99Index) else 0.0,
      minResponseTime = if (sortedTimes.nonEmpty) sortedTimes.head else 0.0,
      maxResponseTime = if (sortedTimes.nonEmpty) sortedTimes.last else 0.0
    )
  }

  def getLatestResult(prefix: String): Option[String] = {
    val resultsDir = new File("gatling/results")
    if (!resultsDir.exists()) {
      println(s"❌ Results directory not found: ${resultsDir.getAbsolutePath}")
      return None
    }

    val dirs = resultsDir.listFiles()
      .filter(_.isDirectory)
      .filter(_.getName.startsWith(prefix))
      .filter(_.listFiles().exists(_.getName == "simulation.log"))
      .sortBy(_.getName)
      .reverse

    dirs.headOption.map(_.getAbsolutePath + "/simulation.log")
  }

  def compareLatest(): Unit = {
    println("\n" + "=" * 80)
    println("СРАВНИТЕЛЬНЫЙ АНАЛИЗ РЕЗУЛЬТАТОВ")
    println("=" * 80)

    val baselineLog = getLatestResult("baselinetest")
    val regressionLog = getLatestResult("regressiontest")

    if (baselineLog.isEmpty || regressionLog.isEmpty) {
      println("\n❌ Не найдены логи тестов!")
      println("Убедитесь, что:")
      println("  1. Тесты были запущены")
      println("  2. Результаты находятся в папке gatling/results/")
      println("  3. Имена папок начинаются с 'baselinetest' и 'regressiontest'")
      return
    }

    try {
      val baseline = parseSimulationLog(baselineLog.get)
      val regression = parseSimulationLog(regressionLog.get)

      println("\n" + "-" * 80)
      println(f"${"Метрика"}%-30s ${"Эталон (1080)"}%18s ${"Регрессия (1090)"}%18s ${"Отклонение"}%15s")
      println("-" * 80)

      // Сравнение каждой метрики
      def printComparison(label: String, baseVal: Double, regVal: Double, format: String = "%.0f"): Unit = {
        val diff = if (baseVal != 0) ((regVal - baseVal) / baseVal) * 100 else 0.0
        val status = if (Math.abs(diff) > 5) "⚠️" else "✅"
        println(f"$label%-30s $baseVal%18$format $regVal%18$format ${diff}%+14.1f%% $status")
      }

      printComparison("Всего запросов", baseline.totalRequests, regression.totalRequests, "%.0f")
      printComparison("Успешных запросов", baseline.successfulRequests, regression.successfulRequests, "%.0f")
      printComparison("Ошибки", baseline.errorPercent, regression.errorPercent, "%.2f")
      printComparison("Среднее время (мс)", baseline.meanResponseTime, regression.meanResponseTime, "%.0f")
      printComparison("95-й перцентиль", baseline.p95ResponseTime, regression.p95ResponseTime, "%.0f")
      printComparison("99-й перцентиль", baseline.p99ResponseTime, regression.p99ResponseTime, "%.0f")
      printComparison("Минимальное время", baseline.minResponseTime, regression.minResponseTime, "%.0f")
      printComparison("Максимальное время", baseline.maxResponseTime, regression.maxResponseTime, "%.0f")

      println("-" * 80)

      // Заключение
      val meanDegradation = ((regression.meanResponseTime - baseline.meanResponseTime) / baseline.meanResponseTime) * 100
      val errorIncrease = regression.errorPercent - baseline.errorPercent

      println("\n📋 ЗАКЛЮЧЕНИЕ:")
      if (meanDegradation > 10) {
        println(f"  ❌ СРЕДНЕЕ ВРЕМЯ ВЫРОСЛО НА ${meanDegradation}%.1f%% (>10%) - ДЕГРАДАЦИЯ")
      } else if (meanDegradation > 5) {
        println(f"  ⚠️ СРЕДНЕЕ ВРЕМЯ ВЫРОСЛО НА ${meanDegradation}%.1f%% (5-10%) - НЕЗНАЧИТЕЛЬНАЯ ДЕГРАДАЦИЯ")
      } else if (meanDegradation < -5) {
        println(f"  ✅ СРЕДНЕЕ ВРЕМЯ СНИЗИЛОСЬ НА ${Math.abs(meanDegradation)}%.1f%% - УЛУЧШЕНИЕ")
      } else {
        println(f"  ✅ СРЕДНЕЕ ВРЕМЯ ИЗМЕНИЛОСЬ НА ${meanDegradation}%.1f%% (<5%) - БЕЗ ИЗМЕНЕНИЙ")
      }

      if (errorIncrease > 1) {
        println(f"  ❌ ОШИБКИ ВЫРОСЛИ НА ${errorIncrease}%.2f%% (>1%) - ПРОБЛЕМА")
      } else if (errorIncrease > 0.5) {
        println(f"  ⚠️ ОШИБКИ ВЫРОСЛИ НА ${errorIncrease}%.2f%% (0.5-1%) - ВНИМАНИЕ")
      } else {
        println(f"  ✅ ОШИБКИ В ПРЕДЕЛАХ НОРМЫ (${errorIncrease}%+.2f%%)")
      }

      // Итоговый вердикт
      println("\n" + "=" * 80)
      if (meanDegradation > 10 || errorIncrease > 1) {
        println("🚫 ВЕРДИКТ: РЕЛИЗ НЕ РЕКОМЕНДУЕТСЯ - ЕСТЬ КРИТИЧЕСКАЯ ДЕГРАДАЦИЯ")
      } else if (meanDegradation > 5 || errorIncrease > 0.5) {
        println("⚠️ ВЕРДИКТ: РЕЛИЗ ВОЗМОЖЕН С ОГОВОРКАМИ - ТРЕБУЕТСЯ ДОРАБОТКА")
      } else {
        println("✅ ВЕРДИКТ: РЕЛИЗ РЕКОМЕНДУЕТСЯ - ДЕГРАДАЦИИ НЕТ")
      }
      println("=" * 80 + "\n")

    } catch {
      case e: Exception =>
        println(s"\n❌ Ошибка при парсинге логов: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  // Основной метод для запуска
  def main(args: Array[String]): Unit = {
    compareLatest()
  }
}
