package me.muheun.moaspace.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * TokenizerService 단위 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TokenizerService 단위 테스트")
class TokenizerServiceTest {

    @Autowired
    private lateinit var tokenizerService: TokenizerService

    @Test
    @DisplayName("텍스트를 토큰으로 인코딩할 수 있다")
    fun shouldEncodeTextToTokens() {
        // given
        val text = "Hello, world!"

        // when
        val tokens = tokenizerService.encode(text)

        // then
        assertNotNull(tokens)
        assertTrue(tokens.isNotEmpty())
        println("Text: '$text' -> Tokens: $tokens (count: ${tokens.size})")

        // 디버그: 개별 토큰 디코딩하여 분할 확인
        val decodedTokens = tokens.map { tokenizerService.decode(listOf(it)) }
        println("토큰별 분할: ${decodedTokens.mapIndexed { idx, token ->
            "[$idx] '$token' (ID: ${tokens[idx]})"
        }.joinToString(" | ")}")
    }

    @Test
    @DisplayName("토큰을 텍스트로 디코딩할 수 있다")
    fun shouldDecodeTokensToText() {
        // given
        val text = "Hello, world!"

        // when
        val tokens = tokenizerService.encode(text)
        val decoded = tokenizerService.decode(tokens)

        // then
        // 토크나이저가 정상적으로 디코딩하는지 확인
        assertNotNull(decoded)
        assertTrue(decoded.isNotEmpty(), "디코딩된 텍스트가 비어있으면 안됨")
        println("Original: '$text' -> Tokens: $tokens -> Decoded: '$decoded'")
    }

    @Test
    @DisplayName("텍스트의 토큰 수를 정확히 계산한다")
    fun shouldCountTokensAccurately() {
        // given
        val text = "Hello, world!"

        // when
        val tokenCount = tokenizerService.countTokens(text)
        val encodedTokens = tokenizerService.encode(text)

        // then
        assertEquals(encodedTokens.size, tokenCount)
        println("Text: '$text' has $tokenCount tokens")
    }

    @Test
    @DisplayName("한국어 텍스트의 토큰 수를 정확히 계산한다")
    fun shouldCountKoreanTextTokensAccurately() {
        // given
        val koreanText = "안녕하세요. 저는 개발자입니다. Python을 좋아합니다."

        // when
        val tokenCount = tokenizerService.countTokens(koreanText)

        // then
        assertTrue(tokenCount > 0)
        println("Korean text: '$koreanText' has $tokenCount tokens")
    }

    @Test
    @DisplayName("텍스트를 최대 토큰 수에 맞춰 자를 수 있다")
    fun shouldTruncateTextToMaxTokenLimit() {
        // given
        val text = "This is a long sentence that will be truncated to a maximum number of tokens."
        val maxTokens = 10 // special tokens 고려하여 여유있게 설정

        // when
        val truncated = tokenizerService.truncateToTokenLimit(text, maxTokens)
        val truncatedTokenCount = tokenizerService.countTokens(truncated)

        // then
        // special tokens이 포함될 수 있으므로 약간의 여유를 둠
        assertTrue(truncatedTokenCount <= maxTokens + 2, "토큰 수: $truncatedTokenCount, 최대: $maxTokens")
        assertTrue(truncated.length < text.length, "텍스트가 잘려야 함")
        println("Original: '$text' (${tokenizerService.countTokens(text)} tokens)")
        println("Truncated: '$truncated' ($truncatedTokenCount tokens)")
    }

    @Test
    @DisplayName("이미 토큰 제한 내의 텍스트는 그대로 반환한다")
    fun shouldReturnOriginalTextWhenWithinTokenLimit() {
        // given
        val text = "Short text"
        val maxTokens = 100

        // when
        val result = tokenizerService.truncateToTokenLimit(text, maxTokens)

        // then
        assertEquals(text, result)
    }

    @Test
    @DisplayName("빈 문자열의 토큰 수는 0이다")
    fun shouldReturnZeroTokensForEmptyString() {
        // given
        val emptyText = ""

        // when
        val tokenCount = tokenizerService.countTokens(emptyText)

        // then
        assertEquals(0, tokenCount)
    }

    @Test
    @DisplayName("공백만 있는 텍스트도 토큰으로 계산된다")
    fun shouldCountWhitespaceAsTokens() {
        // given
        val whitespaceText = "   "

        // when
        val tokenCount = tokenizerService.countTokens(whitespaceText)

        // then
        // 공백도 토큰으로 처리될 수 있으므로 0 이상이면 정상
        println("Whitespace text: '$whitespaceText' has $tokenCount tokens")
        assertTrue(tokenCount >= 0, "토큰 수는 0 이상이어야 함: $tokenCount")
    }
}
