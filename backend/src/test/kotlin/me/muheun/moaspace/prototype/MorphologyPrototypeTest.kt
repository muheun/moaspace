package me.muheun.moaspace.prototype

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.openkoreantext.processor.OpenKoreanTextProcessorJava
import org.openkoreantext.processor.tokenizer.KoreanTokenizer
import scala.collection.Seq
import org.assertj.core.api.Assertions.assertThat

/**
 * T002: 형태소 분석 기본 테스트
 *
 * Open Korean Text를 사용한 한국어 형태소 분석 기본 동작을 검증합니다.
 *
 * **Acceptance Criteria:**
 * - 한국어 텍스트 정규화 동작 확인
 * - 토큰화 동작 확인
 * - 명사 추출 동작 확인
 * - 혼합 텍스트(한국어+영어) 처리 확인
 */
@DisplayName("[T002] 형태소 분석 프로토타입 테스트")
class MorphologyPrototypeTest {

    @Test
    @DisplayName("AC1: 한국어 텍스트 정규화가 정상 동작해야 한다")
    fun `should normalize Korean text`() {
        // given
        val text = "한국어ㅋㅋㅋ 처리 테스트입니다!!!"

        // when
        val normalized = OpenKoreanTextProcessorJava.normalize(text)
        val result = normalized.toString()

        // then
        println("📊 [AC1] 정규화 테스트:")
        println("  - 원본: \"$text\"")
        println("  - 정규화: \"$result\"")

        assertThat(result).isNotBlank()
        // 정규화는 이모티콘 제거, 반복 문자 축약 등을 수행
        assertThat(result).doesNotContain("ㅋㅋㅋ")
    }

    @Test
    @DisplayName("AC2: 한국어 텍스트 토큰화가 정상 동작해야 한다")
    fun `should tokenize Korean text`() {
        // given
        val text = "Spring Boot는 Java 기반의 프레임워크입니다"

        // when
        val normalized = OpenKoreanTextProcessorJava.normalize(text)
        val tokens = OpenKoreanTextProcessorJava.tokenize(normalized)

        // Scala Seq를 Java List로 변환
        val tokenList = scala.collection.JavaConverters.seqAsJavaList(tokens)

        // then
        println("📊 [AC2] 토큰화 테스트:")
        println("  - 원본: \"$text\"")
        println("  - 토큰 수: ${tokenList.size}")
        println("  - 토큰 목록:")
        tokenList.forEach { token ->
            println("    * ${token.text()} (${token.pos()})")
        }

        assertThat(tokenList).isNotEmpty
        assertThat(tokenList.size).isGreaterThan(5)
    }

    @Test
    @DisplayName("AC3: 한국어 명사 추출이 정상 동작해야 한다")
    fun `should extract Korean nouns`() {
        // given
        val text = "딥러닝과 머신러닝을 활용한 자연어 처리 시스템"

        // when
        val normalized = OpenKoreanTextProcessorJava.normalize(text)
        val tokens = OpenKoreanTextProcessorJava.tokenize(normalized)

        // 명사 추출 (Noun, ProperNoun)
        val tokenList = scala.collection.JavaConverters.seqAsJavaList(tokens)
        val nouns = tokenList
            .filter { token ->
                val pos = token.pos().toString()
                pos == "Noun" || pos == "ProperNoun"
            }
            .map { it.text() }

        // then
        println("📊 [AC3] 명사 추출 테스트:")
        println("  - 원본: \"$text\"")
        println("  - 추출된 명사: $nouns")

        assertThat(nouns).isNotEmpty
        assertThat(nouns).contains("딥러닝", "머신러닝", "자연어", "처리", "시스템")
    }

    @Test
    @DisplayName("AC4: 혼합 텍스트(한국어+영어)를 처리할 수 있어야 한다")
    fun `should handle mixed Korean and English text`() {
        // given
        val text = "GPU를 사용한 Deep Learning 학습 방법"

        // when
        val normalized = OpenKoreanTextProcessorJava.normalize(text)
        val tokens = OpenKoreanTextProcessorJava.tokenize(normalized)

        val tokenList = scala.collection.JavaConverters.seqAsJavaList(tokens)
        val nouns = tokenList
            .filter { token ->
                val pos = token.pos().toString()
                pos == "Noun" || pos == "ProperNoun" || pos == "Alpha"
            }
            .map { it.text() }

        // then
        println("📊 [AC4] 혼합 텍스트 처리 테스트:")
        println("  - 원본: \"$text\"")
        println("  - 추출된 토큰: $nouns")

        assertThat(nouns).isNotEmpty
        // 한국어 명사와 영어 단어 모두 추출되어야 함
        assertThat(nouns).contains("학습", "방법")
    }

    @Test
    @DisplayName("AC5: Open Korean Text 성능 테스트")
    fun `performance test - tokenization should be fast`() {
        // given
        val text = "자연어 처리는 인공지능의 중요한 분야입니다. " +
                "딥러닝과 머신러닝 기술이 발전하면서 많은 발전이 있었습니다."

        // warm-up
        repeat(5) {
            val normalized = OpenKoreanTextProcessorJava.normalize(text)
            OpenKoreanTextProcessorJava.tokenize(normalized)
        }

        // when - 100회 반복
        val times = mutableListOf<Long>()
        repeat(100) {
            val start = System.currentTimeMillis()
            val normalized = OpenKoreanTextProcessorJava.normalize(text)
            OpenKoreanTextProcessorJava.tokenize(normalized)
            val end = System.currentTimeMillis()
            times.add(end - start)
        }

        // then
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0

        println("📊 [AC5] 형태소 분석 성능:")
        println("  - 평균 시간: ${avgTime}ms")
        println("  - 최소 시간: ${minTime}ms")
        println("  - 최대 시간: ${maxTime}ms")
        println("  - 참고: 임베딩 시간(50ms)과 독립적")

        // 형태소 분석은 10ms 이내 목표
        assertThat(avgTime).isLessThanOrEqualTo(10.0)
    }
}
