package me.muheun.moaspace.service.impl

import me.muheun.moaspace.service.MorphologyService
import org.openkoreantext.processor.OpenKoreanTextProcessorJava
import org.openkoreantext.processor.tokenizer.KoreanTokenizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(HybridMorphologyService::class.java)

/**
 * 한영 혼합 형태소 분석 서비스
 *
 * Open Korean Text를 사용하여 한국어 형태소 분석을 수행하고,
 * 정규식을 사용하여 영어 단어를 처리합니다.
 *
 * 주요 기능:
 * - 한국어: Open Korean Text 기반 명사 추출
 * - 영어: 정규식 패턴 매칭 (`\b[A-Za-z]{2,}\b`)
 * - 한영 혼합 텍스트: 하이브리드 처리
 */
@Service
class HybridMorphologyService : MorphologyService {

    companion object {
        // 영어 단어 매칭 정규식 (2글자 이상)
        private val ENGLISH_WORD_PATTERN = Regex("\\b[A-Za-z]{2,}\\b")

        // 최소 키워드 길이
        private const val MIN_KEYWORD_LENGTH = 2
    }

    /**
     * 텍스트를 토큰으로 분리
     *
     * Open Korean Text의 토큰화를 사용합니다.
     *
     * @param text 토큰화할 텍스트
     * @return List<String> 토큰 목록
     */
    override fun tokenize(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        return try {
            val normalized = OpenKoreanTextProcessorJava.normalize(text)
            val tokens = OpenKoreanTextProcessorJava.tokenize(normalized)

            // Scala Seq를 Kotlin List로 변환
            val tokenList = mutableListOf<String>()
            val iterator = tokens.iterator()
            while (iterator.hasNext()) {
                tokenList.add(iterator.next().text())
            }
            tokenList

        } catch (e: Exception) {
            logger.warn("토큰화 실패, 공백 기반 분리로 폴백: {}", e.message)
            text.split(Regex("\\s+")).filter { it.isNotBlank() }
        }
    }

    /**
     * 텍스트 정규화
     *
     * Open Korean Text의 정규화를 사용합니다.
     * - 반복되는 문자 정리
     * - 이모지 정리
     * - 공백 정리
     *
     * @param text 정규화할 텍스트
     * @return String 정규화된 텍스트
     */
    override fun normalize(text: String): String {
        if (text.isBlank()) return ""

        return try {
            val normalized = OpenKoreanTextProcessorJava.normalize(text)
            normalized.toString().trim()

        } catch (e: Exception) {
            logger.warn("정규화 실패, 기본 trim으로 폴백: {}", e.message)
            text.trim()
        }
    }

    /**
     * 명사 추출 (한영 혼합 하이브리드)
     *
     * 1. Open Korean Text로 한국어 명사 추출
     * 2. 정규식으로 영어 단어 추출
     * 3. 중복 제거 및 통합
     *
     * @param text 명사를 추출할 텍스트
     * @return List<String> 추출된 명사 목록
     */
    override fun extractNouns(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val nouns = mutableSetOf<String>()

        try {
            // 1. 한국어 명사 추출 (Open Korean Text)
            val koreanNouns = extractKoreanNouns(text)
            nouns.addAll(koreanNouns)

            // 2. 영어 단어 추출 (정규식)
            val englishWords = extractEnglishWords(text)
            nouns.addAll(englishWords)

            logger.debug("명사 추출 완료: 한국어={}, 영어={}, 전체={}",
                koreanNouns.size, englishWords.size, nouns.size)

        } catch (e: Exception) {
            logger.error("명사 추출 중 오류 발생: {}", e.message, e)
        }

        return nouns.filter { it.length >= MIN_KEYWORD_LENGTH }.toList()
    }

    /**
     * 키워드 추출 (빈도 기반)
     *
     * 명사를 추출하고 빈도를 계산하여 상위 N개를 반환합니다.
     *
     * @param text 키워드를 추출할 텍스트
     * @param topN 추출할 키워드 개수 (기본값: 10)
     * @return List<Pair<String, Int>> 키워드와 빈도 쌍의 목록 (빈도 순 정렬)
     */
    override fun extractKeywords(text: String, topN: Int): List<Pair<String, Int>> {
        val nouns = extractNouns(text)

        // 빈도 계산
        val frequencyMap = nouns.groupingBy { it }.eachCount()

        // 빈도 순 정렬 후 상위 N개 반환
        return frequencyMap.entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key to it.value }
    }

    /**
     * 한국어 명사 추출 (Open Korean Text)
     *
     * @param text 텍스트
     * @return List<String> 한국어 명사 목록
     */
    private fun extractKoreanNouns(text: String): List<String> {
        return try {
            val normalized = OpenKoreanTextProcessorJava.normalize(text)
            val tokens = OpenKoreanTextProcessorJava.tokenize(normalized)

            // Scala Seq를 Kotlin List로 변환
            val tokenList = mutableListOf<String>()
            val iterator = tokens.iterator()
            while (iterator.hasNext()) {
                val token = iterator.next()
                val pos = token.pos().toString()

                // 명사 품사 태그: Noun (일반 명사), ProperNoun (고유명사)
                if (pos == "Noun" || pos == "ProperNoun") {
                    val tokenText = token.text()
                    if (tokenText.length >= MIN_KEYWORD_LENGTH) {
                        tokenList.add(tokenText)
                    }
                }
            }

            tokenList

        } catch (e: Exception) {
            logger.warn("한국어 명사 추출 실패: {}", e.message)
            emptyList()
        }
    }

    /**
     * 영어 단어 추출 (정규식)
     *
     * 2글자 이상의 영어 단어를 추출합니다.
     * 소문자로 변환하여 반환합니다.
     *
     * @param text 텍스트
     * @return List<String> 영어 단어 목록
     */
    private fun extractEnglishWords(text: String): List<String> {
        return ENGLISH_WORD_PATTERN.findAll(text)
            .map { it.value.lowercase() }
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .toList()
    }
}
