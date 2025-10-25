package me.muheun.moaspace.service

import org.springframework.stereotype.Service

/**
 * 문자 기반 토크나이저 서비스
 *
 * 문자 길이를 기반으로 토큰 수를 추정합니다.
 * 실제 토큰 카운트가 아닌 추정치이므로 청킹 용도로만 사용합니다.
 *
 * 주요 기능:
 * - 텍스트 길이 기반 토큰 수 추정
 * - 최대 토큰 수에 맞춰 텍스트 자르기
 *
 * 레퍼런스: vector_server_exam의 FixedSizeChunkingService
 */
@Service
class TokenizerService {

    companion object {
        // 평균적으로 한 토큰은 약 3.5자 (한국어/영어 혼합 기준)
        private const val CHARS_PER_TOKEN = 3.5
    }

    /**
     * 텍스트의 토큰 수 추정
     *
     * @param text 토큰 수를 계산할 텍스트
     * @return 추정 토큰 수
     */
    fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0
        return (text.length / CHARS_PER_TOKEN).toInt()
    }

    /**
     * 텍스트를 최대 토큰 수에 맞춰 잘라냄
     *
     * 토큰 수가 maxTokens를 초과하면 maxTokens까지만 유지하고 나머지는 버림
     *
     * @param text 자를 텍스트
     * @param maxTokens 최대 토큰 수
     * @return 잘라낸 텍스트
     */
    fun truncateToTokenLimit(text: String, maxTokens: Int): String {
        val estimatedTokens = countTokens(text)
        if (estimatedTokens <= maxTokens) {
            return text
        }

        // 최대 토큰에 해당하는 문자 수 계산
        val maxChars = (maxTokens * CHARS_PER_TOKEN).toInt()

        // 문자 경계에서 자르기 (문장 경계를 고려하지 않고 단순하게)
        return if (text.length > maxChars) {
            text.substring(0, maxChars)
        } else {
            text
        }
    }

    /**
     * 텍스트를 토큰 ID 리스트로 변환 (호환성 유지용)
     *
     * 실제 토큰화가 아닌 문자 인덱스 리스트 반환
     *
     * @param text 변환할 텍스트
     * @return 문자 인덱스 리스트
     */
    fun encode(text: String): List<Int> {
        return text.indices.toList()
    }

    /**
     * 토큰 ID 리스트를 텍스트로 복원 (호환성 유지용)
     *
     * @param tokens 토큰 ID 리스트 (실제로는 인덱스)
     * @return 복원된 텍스트
     */
    fun decode(tokens: List<Int>): String {
        // 실제 토큰화가 아니므로 의미 없는 구현
        // 기존 코드 호환성을 위해 유지
        return ""
    }
}
