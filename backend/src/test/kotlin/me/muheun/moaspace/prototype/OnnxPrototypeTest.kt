package me.muheun.moaspace.prototype

import me.muheun.moaspace.service.VectorEmbeddingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.math.sqrt

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ONNX 임베딩 테스트")
class OnnxPrototypeTest {

    @Autowired
    private lateinit var embeddingService: VectorEmbeddingService

    @Test
    @DisplayName("'안녕하세요'를 768차원 벡터로 임베딩하고 L2 norm이 1.0이어야 한다")
    fun shouldEmbedKoreanTextTo768DimensionalVectorWithL2NormOne() {
        // given
        val text = "안녕하세요"

        // when
        val embedding = embeddingService.generateEmbedding(text)
        val embeddingArray = embedding.toArray().map { it.toFloat() }.toFloatArray()

        // then
        println("📊 임베딩 결과:")
        println("  - 텍스트: \"$text\"")
        println("  - 벡터 차원: ${embeddingArray.size}")
        println("  - 첫 5개 값: ${embeddingArray.take(5)}")

        assertThat(embeddingArray.size).isEqualTo(768) // multilingual-e5-base는 768차원

        // L2 norm = 1.0 ± 0.0001 검증
        val norm = sqrt(embeddingArray.sumOf { (it * it).toDouble() })
        println("  - L2 norm: $norm")

        assertThat(norm).isCloseTo(1.0, within(0.0001))
    }

    @Test
    @DisplayName("단일 문장 임베딩이 50ms 이내여야 한다")
    fun shouldCompleteEmbeddingWithin50MsWhenProcessingSingleSentence() {
        // given
        val text = "Spring Boot는 Java 기반의 강력한 웹 프레임워크입니다."

        // warm-up (모델 로딩 및 JIT 최적화)
        repeat(5) {
            embeddingService.generateEmbedding(text)
        }

        // when - 50회 반복 테스트
        val times = mutableListOf<Long>()
        repeat(50) {
            val start = System.currentTimeMillis()
            embeddingService.generateEmbedding(text)
            val end = System.currentTimeMillis()
            times.add(end - start)
        }

        // then
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0

        println("📊 성능 테스트 결과:")
        println("  - 평균 시간: ${avgTime}ms")
        println("  - 최소 시간: ${minTime}ms")
        println("  - 최대 시간: ${maxTime}ms")
        println("  - 목표: 50ms 이내")
        assertThat(avgTime).describedAs("평균 임베딩 시간이 50ms를 초과했습니다")
            .isLessThanOrEqualTo(50.0)
    }

    @Test
    @DisplayName("의미적으로 유사한 텍스트는 높은 유사도를 가져야 한다")
    fun shouldHaveHighSimilarityWhenTextsSemanticallyRelated() {
        // given
        val text1 = "Python 프로그래밍"
        val text2 = "파이썬 코딩"

        // when
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)

        // then
        val similarity = cosineSimilarity(
            embedding1.toArray().map { it.toFloat() }.toFloatArray(),
            embedding2.toArray().map { it.toFloat() }.toFloatArray()
        )

        println("📊 의미 유사도 테스트:")
        println("  - 텍스트1: \"$text1\"")
        println("  - 텍스트2: \"$text2\"")
        println("  - 코사인 유사도: $similarity")
        println("  - 목표: ≥ 0.7")
        assertThat(similarity).isGreaterThanOrEqualTo(0.7)
    }

    @Test
    @DisplayName("다국어 텍스트 (한국어-영어)의 유사도를 검증한다")
    fun shouldHandleMultilingualTextsWhenComparingKoreanAndEnglish() {
        // given
        val koreanText = "안녕하세요"
        val englishText = "Hello"

        // when
        val koreanEmbedding = embeddingService.generateEmbedding(koreanText)
        val englishEmbedding = embeddingService.generateEmbedding(englishText)

        // then
        val similarity = cosineSimilarity(
            koreanEmbedding.toArray().map { it.toFloat() }.toFloatArray(),
            englishEmbedding.toArray().map { it.toFloat() }.toFloatArray()
        )

        println("📊 다국어 유사도 테스트:")
        println("  - 한국어: \"$koreanText\"")
        println("  - 영어: \"$englishText\"")
        println("  - 코사인 유사도: $similarity")
        assertThat(similarity).describedAs("한국어-영어 번역 유사도가 낮습니다")
            .isGreaterThanOrEqualTo(0.6)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "벡터 차원이 일치해야 합니다" }

        val dotProduct = a.zip(b).sumOf { (x, y) -> (x * y).toDouble() }
        val normA = sqrt(a.sumOf { (it * it).toDouble() })
        val normB = sqrt(b.sumOf { (it * it).toDouble() })

        return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (normA * normB)
    }
}
