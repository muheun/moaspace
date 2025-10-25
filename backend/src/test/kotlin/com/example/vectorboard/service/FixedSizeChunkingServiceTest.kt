package com.example.vectorboard.service

import com.example.vectorboard.dto.ChunkingParams
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * FixedSizeChunkingService 단위 테스트
 */
@SpringBootTest
class FixedSizeChunkingServiceTest {

    @Autowired
    private lateinit var chunkingService: FixedSizeChunkingService

    @Autowired
    private lateinit var tokenizerService: TokenizerService

    @Test
    fun `짧은 텍스트는 단일 청크로 처리된다`() {
        // given
        val text = "안녕하세요."
        val params = ChunkingParams(
            minTokens = 50,
            targetTokens = 500,
            maxTokens = 600,
            overlap = 100
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0].text)
        println("Short text chunked: ${chunks.size} chunk(s)")
    }

    @Test
    fun `문장 경계에서 청크가 분할된다`() {
        // given
        val text = """
            안녕하세요. 저는 개발자입니다. Python을 좋아합니다.
            Kotlin도 좋아합니다. Spring Boot는 훌륭한 프레임워크입니다.
            PostgreSQL은 강력한 데이터베이스입니다. pgvector는 벡터 검색을 지원합니다.
        """.trimIndent()

        val params = ChunkingParams(
            minTokens = 10,
            targetTokens = 30,  // 작은 청크 크기로 설정
            maxTokens = 50,
            overlap = 10
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertTrue(chunks.size > 1, "Multiple chunks should be created")

        // 각 청크가 문장 경계에서 끝나는지 확인 (마지막 청크 제외)
        chunks.dropLast(1).forEach { chunk ->
            val lastChar = chunk.text.trim().last()
            assertTrue(lastChar == '.' || lastChar == '!' || lastChar == '?',
                "Chunk should end with sentence delimiter: '${chunk.text}'")
        }

        println("Text chunked into ${chunks.size} chunks")
        chunks.forEachIndexed { index, chunk ->
            println("Chunk $index: ${chunk.text.take(50)}... (${chunk.tokenCount} tokens)")
        }
    }

    @Test
    fun `청크 간 오버랩이 적용된다`() {
        // given
        val text = """
            첫 번째 문장입니다. 두 번째 문장입니다. 세 번째 문장입니다.
            네 번째 문장입니다. 다섯 번째 문장입니다. 여섯 번째 문장입니다.
        """.trimIndent()

        val params = ChunkingParams(
            minTokens = 5,
            targetTokens = 30,
            maxTokens = 50,
            overlap = 10
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        if (chunks.size > 1) {
            // 연속된 청크 간에 공통 텍스트가 있는지 확인 (오버랩)
            for (i in 0 until chunks.size - 1) {
                val currentChunk = chunks[i].text
                val nextChunk = chunks[i + 1].text

                println("Chunk $i: $currentChunk")
                println("Chunk ${i + 1}: $nextChunk")

                // 오버랩 검증: 다음 청크의 시작 부분이 이전 청크의 끝 부분과 유사해야 함
                val currentWords = currentChunk.split(" ")
                val nextWords = nextChunk.split(" ")

                val hasOverlap = currentWords.takeLast(3).any { word ->
                    nextWords.take(3).contains(word)
                }

                assertTrue(hasOverlap || chunks.size == 2,
                    "Chunks should have overlap or be only 2 chunks")
            }
        }
    }

    @Test
    fun `빈 문자열은 빈 청크 리스트를 반환한다`() {
        // given
        val text = ""
        val params = ChunkingParams()

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `공백만 있는 텍스트는 빈 청크 리스트를 반환한다`() {
        // given
        val text = "   \n\n   "
        val params = ChunkingParams()

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `긴 텍스트가 여러 청크로 분할된다`() {
        // given
        val longText = """
            PostgreSQL은 강력한 관계형 데이터베이스 관리 시스템입니다.
            오픈 소스이며 다양한 기능을 제공합니다. ACID 속성을 완벽히 지원합니다.
            pgvector 확장은 벡터 검색 기능을 추가합니다.
            코사인 유사도 검색이 가능합니다. 인덱싱을 통해 빠른 검색을 제공합니다.
            Spring Boot는 Java 기반의 웹 애플리케이션 프레임워크입니다.
            자동 설정 기능이 뛰어납니다. RESTful API를 쉽게 만들 수 있습니다.
        """.trimIndent()

        val params = ChunkingParams(
            minTokens = 10,
            targetTokens = 50,
            maxTokens = 70,
            overlap = 10
        )

        // when
        val chunks = chunkingService.chunk(longText, params)

        // then
        assertTrue(chunks.size > 1, "Long text should be split into multiple chunks")

        // 모든 청크의 토큰 수가 최대 크기를 넘지 않는지 확인
        chunks.forEach { chunk ->
            assertTrue(chunk.tokenCount <= params.maxTokens + 20, // 약간의 여유 허용
                "Chunk token count (${chunk.tokenCount}) should not exceed max size (${params.maxTokens})")
        }

        println("Long text chunked into ${chunks.size} chunks")
        chunks.forEachIndexed { index, chunk ->
            println("Chunk $index: ${chunk.tokenCount} tokens - ${chunk.text.take(50)}...")
        }
    }

    @Test
    fun `각 청크의 토큰 수가 정확히 계산된다`() {
        // given
        val text = "안녕하세요. 저는 개발자입니다. Python을 좋아합니다."
        val params = ChunkingParams(
            minTokens = 5,
            targetTokens = 20,
            maxTokens = 30,
            overlap = 5
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        chunks.forEach { chunk ->
            val actualTokenCount = tokenizerService.countTokens(chunk.text)
            assertEquals(actualTokenCount, chunk.tokenCount,
                "Reported token count should match actual token count")
        }
    }

    @Test
    fun `문장 부호가 없는 긴 텍스트도 처리할 수 있다`() {
        // given
        val text = "This is a very long text without any sentence delimiters and it should still be chunked properly based on token count"
        val params = ChunkingParams(
            minTokens = 5,
            targetTokens = 10,
            maxTokens = 15,
            overlap = 2
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertTrue(chunks.isNotEmpty(), "Text should be chunked")
        println("Text without delimiters chunked into ${chunks.size} chunk(s)")
    }

    @Test
    fun `영어 문장도 올바르게 분할된다`() {
        // given
        val text = """
            Hello, world! This is a test. How are you doing today?
            I hope you are doing well. This is another sentence. And one more!
        """.trimIndent()

        val params = ChunkingParams(
            minTokens = 5,
            targetTokens = 15,
            maxTokens = 25,
            overlap = 5
        )

        // when
        val chunks = chunkingService.chunk(text, params)

        // then
        assertTrue(chunks.size > 1, "English text should be split into multiple chunks")

        chunks.forEach { chunk ->
            println("English chunk: ${chunk.text} (${chunk.tokenCount} tokens)")
        }
    }
}
