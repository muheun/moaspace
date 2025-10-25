package me.muheun.moaspace.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * TokenizerService 단위 테스트
 */
@SpringBootTest
class TokenizerServiceTest {

    @Autowired
    private lateinit var tokenizerService: TokenizerService

    @Test
    fun `텍스트를 토큰으로 인코딩할 수 있다`() {
        // given
        val text = "Hello, world!"

        // when
        val tokens = tokenizerService.encode(text)

        // then
        assertNotNull(tokens)
        assertTrue(tokens.isNotEmpty())
        println("Text: '$text' -> Tokens: $tokens (count: ${tokens.size})")
    }

    @Test
    fun `토큰을 텍스트로 디코딩할 수 있다`() {
        // given
        val originalText = "Hello, world!"
        val tokens = tokenizerService.encode(originalText)

        // when
        val decodedText = tokenizerService.decode(tokens)

        // then
        assertEquals(originalText, decodedText)
    }

    @Test
    fun `텍스트의 토큰 수를 정확히 계산한다`() {
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
    fun `한국어 텍스트의 토큰 수를 정확히 계산한다`() {
        // given
        val koreanText = "안녕하세요. 저는 개발자입니다. Python을 좋아합니다."

        // when
        val tokenCount = tokenizerService.countTokens(koreanText)

        // then
        assertTrue(tokenCount > 0)
        println("Korean text: '$koreanText' has $tokenCount tokens")
    }

    @Test
    fun `텍스트를 최대 토큰 수에 맞춰 자를 수 있다`() {
        // given
        val text = "This is a long sentence that will be truncated to a maximum number of tokens."
        val maxTokens = 5

        // when
        val truncated = tokenizerService.truncateToTokenLimit(text, maxTokens)
        val truncatedTokenCount = tokenizerService.countTokens(truncated)

        // then
        assertTrue(truncatedTokenCount <= maxTokens)
        println("Original: '$text' (${tokenizerService.countTokens(text)} tokens)")
        println("Truncated: '$truncated' ($truncatedTokenCount tokens)")
    }

    @Test
    fun `이미 토큰 제한 내의 텍스트는 그대로 반환한다`() {
        // given
        val text = "Short text"
        val maxTokens = 100

        // when
        val result = tokenizerService.truncateToTokenLimit(text, maxTokens)

        // then
        assertEquals(text, result)
    }

    @Test
    fun `빈 문자열의 토큰 수는 0이다`() {
        // given
        val emptyText = ""

        // when
        val tokenCount = tokenizerService.countTokens(emptyText)

        // then
        assertEquals(0, tokenCount)
    }

    @Test
    fun `공백만 있는 텍스트도 토큰으로 계산된다`() {
        // given
        val whitespaceText = "   "

        // when
        val tokenCount = tokenizerService.countTokens(whitespaceText)

        // then
        assertTrue(tokenCount > 0)
        println("Whitespace text has $tokenCount tokens")
    }
}
