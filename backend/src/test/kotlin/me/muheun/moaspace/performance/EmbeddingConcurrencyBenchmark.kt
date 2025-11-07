package me.muheun.moaspace.performance

import me.muheun.moaspace.service.OnnxEmbeddingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Semaphore 기반 동시성 제어 성능 벤치마크
 *
 * **목적**: Phase 1(@Synchronized) vs Phase 2(Semaphore) 성능 비교
 * **측정 항목**:
 * - 동시 요청 수별 처리 시간 (1, 4, 10, 50, 100개)
 * - 평균 응답 시간, P50, P95, P99, Max
 * - 처리량 (requests/sec)
 *
 * **현재 설정**: max-concurrent=4 (Semaphore)
 */
@SpringBootTest
class EmbeddingConcurrencyBenchmark @Autowired constructor(
    private val onnxEmbeddingService: OnnxEmbeddingService
) {

    companion object {
        private const val TEST_TEXT = "벡터 임베딩 성능 테스트를 위한 샘플 텍스트입니다. 이 텍스트는 약 50자 정도의 길이를 가집니다."
        private val CONCURRENCY_LEVELS = listOf(1, 4, 10, 50, 100)
    }

    @Test
    @DisplayName("동시 요청 수별 임베딩 처리 성능 벤치마크")
    fun benchmarkConcurrencyLevels() {
        println("\n=== Semaphore 기반 동시성 제어 성능 벤치마크 ===")
        println("설정: max-concurrent=4")
        println("테스트 텍스트 길이: ${TEST_TEXT.length}자\n")

        val results = mutableListOf<BenchmarkResult>()

        CONCURRENCY_LEVELS.forEach { concurrency ->
            val result = runBenchmark(concurrency)
            results.add(result)
            printBenchmarkResult(result)
        }

        println("\n=== 벤치마크 요약 ===")
        printSummaryTable(results)

        // 성능 검증: max-concurrent=4 설정에서 처리량이 단일 스레드 대비 향상되어야 함
        val sequential = results.find { it.concurrency == 1 }!!
        val parallel4 = results.find { it.concurrency == 4 }!!

        val improvement = parallel4.throughput / sequential.throughput

        println("\n=== 성능 향상 분석 ===")
        println("단일 스레드 (concurrency=1): ${sequential.throughput} req/sec")
        println("병렬 처리 (concurrency=4): ${parallel4.throughput} req/sec")
        println("처리량 향상: ${String.format("%.2f", improvement)}배")

        // 검증: max-concurrent=4에서 처리량이 최소 1.5배 이상 향상되어야 함 (시스템 부하 고려)
        assertThat(improvement).isGreaterThanOrEqualTo(1.5)
            .withFailMessage("병렬 처리 성능 향상이 부족합니다: ${String.format("%.2f", improvement)}배 (목표: ≥1.5배)")
    }

    private fun runBenchmark(concurrency: Int): BenchmarkResult {
        val responseTimes = mutableListOf<Long>()
        val executor = Executors.newFixedThreadPool(concurrency)
        val latch = CountDownLatch(concurrency)
        val successCount = AtomicInteger(0)

        val totalTime = measureTimeMillis {
            repeat(concurrency) {
                executor.submit {
                    try {
                        val elapsed = measureTimeMillis {
                            onnxEmbeddingService.generateEmbedding(TEST_TEXT)
                        }
                        synchronized(responseTimes) {
                            responseTimes.add(elapsed)
                        }
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        println("Error in benchmark: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(60, TimeUnit.SECONDS)
        }

        executor.shutdown()

        val sorted = responseTimes.sorted()
        return BenchmarkResult(
            concurrency = concurrency,
            totalRequests = concurrency,
            successCount = successCount.get(),
            totalTime = totalTime,
            responseTimes = sorted
        )
    }

    private fun printBenchmarkResult(result: BenchmarkResult) {
        println("--- Concurrency: ${result.concurrency} ---")
        println("  Total requests: ${result.totalRequests}")
        println("  Success: ${result.successCount}")
        println("  Total time: ${result.totalTime}ms")
        println("  Throughput: ${result.throughput} req/sec")
        println("  Avg response time: ${result.avgResponseTime}ms")
        println("  P50: ${result.p50}ms")
        println("  P95: ${result.p95}ms")
        println("  P99: ${result.p99}ms")
        println("  Max: ${result.maxResponseTime}ms")
        println()
    }

    private fun printSummaryTable(results: List<BenchmarkResult>) {
        println("| Concurrency | Throughput (req/sec) | Avg (ms) | P50 (ms) | P95 (ms) | P99 (ms) | Max (ms) |")
        println("|-------------|----------------------|----------|----------|----------|----------|----------|")
        results.forEach { r ->
            println("| ${String.format("%11d", r.concurrency)} | ${String.format("%20.2f", r.throughput)} | " +
                    "${String.format("%8.2f", r.avgResponseTime)} | ${String.format("%8d", r.p50)} | " +
                    "${String.format("%8d", r.p95)} | ${String.format("%8d", r.p99)} | ${String.format("%8d", r.maxResponseTime)} |")
        }
    }

    data class BenchmarkResult(
        val concurrency: Int,
        val totalRequests: Int,
        val successCount: Int,
        val totalTime: Long,
        val responseTimes: List<Long>
    ) {
        val throughput: Double get() = if (totalTime > 0) (successCount.toDouble() / totalTime) * 1000 else 0.0
        val avgResponseTime: Double get() = if (responseTimes.isNotEmpty()) responseTimes.average() else 0.0
        val p50: Long get() = percentile(50)
        val p95: Long get() = percentile(95)
        val p99: Long get() = percentile(99)
        val minResponseTime: Long get() = responseTimes.minOrNull() ?: 0
        val maxResponseTime: Long get() = responseTimes.maxOrNull() ?: 0

        private fun percentile(p: Int): Long {
            if (responseTimes.isEmpty()) return 0
            val index = ((p / 100.0) * (responseTimes.size - 1)).toInt()
            return responseTimes[index]
        }
    }
}
