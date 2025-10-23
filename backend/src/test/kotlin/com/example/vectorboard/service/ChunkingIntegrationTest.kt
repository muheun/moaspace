package com.example.vectorboard.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * 청킹 시스템 통합 테스트
 */
@SpringBootTest
class ChunkingIntegrationTest {

    @Autowired
    private lateinit var markdownService: MarkdownService

    @Autowired
    private lateinit var chunkingService: ChunkingService

    @Test
    fun `마크다운을 순수 텍스트로 변환한다`() {
        // Given
        val markdown = """
            # 제목

            이것은 **굵은 글씨**입니다.

            - 리스트 항목 1
            - 리스트 항목 2

            [링크](https://example.com)
        """.trimIndent()

        // When
        val plainText = markdownService.toPlainText(markdown)

        // Then
        assertNotNull(plainText)
        assertTrue(plainText.contains("제목"))
        assertTrue(plainText.contains("굵은 글씨"))
        assertTrue(plainText.contains("리스트 항목"))
        assertFalse(plainText.contains("**"))  // 마크다운 문법 제거
        assertFalse(plainText.contains("#"))   // 마크다운 문법 제거
    }

    @Test
    fun `짧은 텍스트는 청킹하지 않는다`() {
        // Given
        val shortText = "이것은 짧은 텍스트입니다."

        // When
        val chunks = chunkingService.chunkDocument(shortText)

        // Then
        assertEquals(1, chunks.size)
        assertEquals(shortText, chunks[0].text)
        assertEquals(0, chunks[0].index)
    }

    @Test
    fun `긴 텍스트는 여러 청크로 분할된다`() {
        // Given
        val longText = buildString {
            repeat(10) { i ->
                append("문단 $i: ")
                append("이것은 충분히 긴 텍스트입니다. ".repeat(20))
                append("\n\n")
            }
        }

        // When
        val chunks = chunkingService.chunkDocument(longText)

        // Then
        assertTrue(chunks.size > 1, "긴 텍스트는 여러 청크로 분할되어야 합니다")

        // 청크 인덱스 검증
        chunks.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.index)
        }

        // 청크 크기 검증 (너무 크거나 작지 않아야 함)
        chunks.forEach { chunk ->
            assertTrue(chunk.text.length >= 100, "청크가 너무 작습니다: ${chunk.text.length}")
            assertTrue(chunk.text.length <= 2000, "청크가 너무 큽니다: ${chunk.text.length}")
        }
    }

    @Test
    fun `마크다운 문서를 청킹한다`() {
        // Given
        val markdown = """
            # Spring Boot 가이드

            Spring Boot는 자바 기반 웹 애플리케이션을 빠르게 개발할 수 있게 해주는 프레임워크입니다.

            ## 주요 특징

            1. **자동 설정**: 개발자가 설정해야 할 것들을 자동으로 설정해줍니다.
            2. **내장 서버**: Tomcat, Jetty 등의 서버가 내장되어 있습니다.
            3. **스타터 의존성**: 필요한 라이브러리들을 간편하게 추가할 수 있습니다.

            ## 시작하기

            Spring Boot 프로젝트를 시작하는 방법은 다음과 같습니다:

            ```kotlin
            @SpringBootApplication
            class Application

            fun main(args: Array<String>) {
                runApplication<Application>(*args)
            }
            ```

            이렇게 간단하게 애플리케이션을 시작할 수 있습니다.
        """.trimIndent()

        // When
        val plainText = markdownService.toPlainText(markdown)
        val chunks = chunkingService.chunkDocument(plainText)

        // Then
        assertNotNull(plainText)
        assertTrue(chunks.isNotEmpty())

        // 모든 청크를 합치면 원본 텍스트의 주요 내용을 포함해야 함
        val allChunksText = chunks.joinToString(" ") { it.text }
        assertTrue(allChunksText.contains("Spring Boot"))
        assertTrue(allChunksText.contains("자동 설정"))
    }

    @Test
    fun `빈 텍스트는 빈 청크 리스트를 반환한다`() {
        // Given
        val emptyText = ""

        // When
        val chunks = chunkingService.chunkDocument(emptyText)

        // Then
        assertTrue(chunks.isEmpty())
    }
}
