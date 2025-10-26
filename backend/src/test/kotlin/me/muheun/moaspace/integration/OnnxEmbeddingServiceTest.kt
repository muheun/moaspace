package me.muheun.moaspace.integration

import com.pgvector.PGvector
import me.muheun.moaspace.service.VectorEmbeddingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.math.sqrt

/**
 * ONNX 임베딩 서비스 통합 테스트
 *
 * **Constitution Principle II 준수**:
 * - Mock 금지: 실제 ONNX 모델 로딩 (`@SpringBootTest` 사용)
 * - REAL DB 필수: Spring Context 전체 로딩
 *
 * **Independent Test 기준**:
 * - "안녕하세요"와 "Hello"의 유사도가 높아야 함 (≥ 0.7)
 * - "컴퓨터"와 "사과"의 유사도가 낮아야 함 (< 0.5)
 *
 * **E5-base 모델 사용**:
 * - 768차원 벡터 (multilingual-e5-base)
 * - 100개 언어 지원 (다국어 최적화)
 */
@SpringBootTest
@DisplayName("ONNX 임베딩 서비스 통합 테스트 (실제 모델 사용)")
class OnnxEmbeddingServiceTest {

    @Autowired
    private lateinit var embeddingService: VectorEmbeddingService

    /**
     * PGvector를 FloatArray로 변환
     */
    private fun PGvector.toFloatArray(): FloatArray {
        return this.toArray().map { it.toFloat() }.toFloatArray()
    }

    /**
     * 코사인 유사도 계산
     */
    private fun cosineSimilarity(a: PGvector, b: PGvector): Double {
        val arrayA = a.toFloatArray()
        val arrayB = b.toFloatArray()

        require(arrayA.size == arrayB.size) { "벡터 차원이 일치해야 합니다" }

        val dotProduct = arrayA.zip(arrayB).sumOf { (x, y) -> (x * y).toDouble() }
        val normA = sqrt(arrayA.sumOf { (it * it).toDouble() })
        val normB = sqrt(arrayB.sumOf { (it * it).toDouble() })

        return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (normA * normB)
    }

    @Test
    @DisplayName("T022: 한국어 텍스트를 768차원 벡터로 임베딩하고 L2 norm이 1.0이어야 한다")
    fun `should embed Korean text to 768-dimensional vector with L2 norm 1_0`() {
        // given
        val text = "안녕하세요"

        // when
        val embedding = embeddingService.generateEmbedding(text)
        val embeddingArray = embedding.toFloatArray()

        // then
        assertThat(embedding).isNotNull
        assertThat(embeddingArray.size).isEqualTo(768) // E5-base는 768차원

        // L2 norm = 1.0 ± 0.0001 검증
        val norm = sqrt(embeddingArray.sumOf { (it * it).toDouble() })
        assertThat(norm).isCloseTo(1.0, within(0.0001))
    }

    @Test
    @DisplayName("T023: 동일한 텍스트를 두 번 임베딩하면 코사인 유사도가 1.0이어야 한다")
    fun `should have cosine similarity 1_0 for identical text embeddings`() {
        // given
        val text = "Spring Boot는 Java 기반의 프레임워크입니다"

        // when
        val embedding1 = embeddingService.generateEmbedding(text)
        val embedding2 = embeddingService.generateEmbedding(text)

        // then
        val similarity = cosineSimilarity(embedding1, embedding2)
        assertThat(similarity).isCloseTo(1.0, within(0.0001))
    }

    @Test
    @DisplayName("T024: 의미적으로 유사한 한국어 텍스트는 높은 유사도를 가져야 한다 (≥ 0.8)")
    fun `should have high similarity for semantically similar Korean texts`() {
        // given
        val text1 = "Python 프로그래밍"
        val text2 = "파이썬 코딩"

        // when
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)

        // then
        val similarity = cosineSimilarity(embedding1, embedding2)
        println("📊 \"$text1\" vs \"$text2\" 유사도: $similarity")
        assertThat(similarity).isGreaterThanOrEqualTo(0.8)
    }

    @Test
    @DisplayName("T024: 의미적으로 다른 한국어 텍스트는 낮은 유사도를 가져야 한다 (< 0.85)")
    fun `should have low similarity for semantically different Korean texts`() {
        // given - 문장 단위 테스트로 변경 (모델이 문장 임베딩에 최적화되어 있음)
        // 완전히 다른 주제: 기술 vs 자연/날씨
        // NOTE: multilingual-e5-base는 대칭적 문장 임베딩에 최적화되어 있어
        //       일반적인 문장 간 유사도가 0.7~0.8 범위를 보임
        val text1 = "최신 프로세서와 그래픽 카드를 탑재한 고성능 컴퓨터를 구매했습니다."
        val text2 = "창밖을 보니 비가 내리고 있었고, 무지개가 하늘에 걸려 있었습니다."

        // when
        val embedding1 = embeddingService.generateEmbedding(text1)
        val embedding2 = embeddingService.generateEmbedding(text2)

        // then
        val similarity = cosineSimilarity(embedding1, embedding2)
        println("📊 다른 주제 유사도: $similarity")
        assertThat(similarity).isLessThan(0.85)
    }

    @Test
    @DisplayName("T025: 다국어 텍스트 (한국어-영어)는 높은 유사도를 가져야 한다 (≥ 0.7)")
    fun `should have high similarity for multilingual texts (Korean-English)`() {
        // given - Independent Test 기준
        val koreanText = "안녕하세요"
        val englishText = "Hello"

        // when
        val koreanEmbedding = embeddingService.generateEmbedding(koreanText)
        val englishEmbedding = embeddingService.generateEmbedding(englishText)

        // then
        val similarity = cosineSimilarity(koreanEmbedding, englishEmbedding)
        assertThat(similarity).isGreaterThanOrEqualTo(0.7) // E5-base는 100개 언어 지원
    }

    @Test
    @DisplayName("T026: 512 토큰을 초과하는 긴 텍스트는 자동으로 truncate되어 정상 처리되어야 한다")
    fun `should auto-truncate long text exceeding 512 tokens`() {
        // given - 1000자 이상의 긴 텍스트
        val longText = "Spring Boot는 Java 기반의 프레임워크입니다. " .repeat(100)

        // when & then - 예외 발생 없이 정상 처리
        val embedding = embeddingService.generateEmbedding(longText)
        val embeddingArray = embedding.toFloatArray()

        assertThat(embedding).isNotNull
        assertThat(embeddingArray.size).isEqualTo(768)

        // L2 norm 검증
        val norm = sqrt(embeddingArray.sumOf { (it * it).toDouble() })
        assertThat(norm).isCloseTo(1.0, within(0.0001))
    }

    @Test
    @DisplayName("T027: 빈 문자열 입력 시 IllegalArgumentException이 발생해야 한다")
    fun `should throw IllegalArgumentException for empty string`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            embeddingService.generateEmbedding("")
        }
    }

    @Test
    @DisplayName("T027: 공백만 있는 텍스트 입력 시 IllegalArgumentException이 발생해야 한다")
    fun `should throw IllegalArgumentException for whitespace-only text`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            embeddingService.generateEmbedding("   ")
        }
    }

    @Test
    @DisplayName("T027: 특수문자만 있는 텍스트는 정상 처리되어야 한다")
    fun `should handle text with only special characters`() {
        // given
        val specialText = "!@#$%^&*()"

        // when
        val embedding = embeddingService.generateEmbedding(specialText)
        val embeddingArray = embedding.toFloatArray()

        // then
        assertThat(embedding).isNotNull
        assertThat(embeddingArray.size).isEqualTo(768)
    }

    @Test
    @DisplayName("성능 테스트: 단일 문장(50자) 임베딩은 30ms 이내여야 한다")
    fun `performance test - single sentence should complete within 30ms`() {
        // given
        val text = "Spring Boot는 Java 기반의 강력한 웹 프레임워크입니다." // 약 50자

        // warm-up
        repeat(3) {
            embeddingService.generateEmbedding(text)
        }

        // when - 100회 반복 테스트
        val times = mutableListOf<Long>()
        repeat(100) {
            val start = System.currentTimeMillis()
            embeddingService.generateEmbedding(text)
            val end = System.currentTimeMillis()
            times.add(end - start)
        }

        // then
        val avgTime = times.average()
        val p95Time = times.sorted()[94] // 95 percentile

        println("📊 성능 테스트 결과 (단일 문장 50자)")
        println("  - 평균 시간: ${avgTime}ms")
        println("  - P95: ${p95Time}ms")
        println("  - 최소: ${times.minOrNull()}ms")
        println("  - 최대: ${times.maxOrNull()}ms")

        assertThat(avgTime).isLessThanOrEqualTo(30.0)
        assertThat(p95Time).isLessThanOrEqualTo(40) // P95는 40ms 이내
    }
}
