package me.muheun.moaspace.service

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

    @Autowired
    private lateinit var fixedSizeChunkingService: FixedSizeChunkingService

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

    // ========== Phase 4 - User Story 4: 스마트 청킹 품질 향상 테스트 ==========

    @Test
    fun `US4-AC1 - 1000자 텍스트는 문장 경계에서 분할된다`() {
        // Given: 여러 문장으로 구성된 1000자 텍스트
        val text = buildString {
            repeat(20) { i ->
                append("이것은 문장 ${i}입니다. ")
                append("문장은 항상 마침표로 끝나야 합니다. ")
                append("중간에 문장이 잘려서는 안 됩니다. ")
            }
        }
        assertTrue(text.length >= 1000, "텍스트 길이가 1000자 이상이어야 합니다")

        // When: FixedSizeChunkingService로 청킹
        val chunks = fixedSizeChunkingService.chunk(text)

        // Then: 문장 경계에서 분할 검증
        chunks.forEach { chunk ->
            // 청크가 문장 부호로 끝나는지 확인 (마지막 청크 제외)
            val trimmed = chunk.text.trim()
            if (chunk.chunkIndex < chunks.size - 1) {
                assertTrue(
                    trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?"),
                    "청크는 문장 부호로 끝나야 합니다: '${trimmed.takeLast(20)}'"
                )
            }

            // 청크 내부에 불완전한 문장이 없는지 확인
            // (문장 부호 이후 공백과 대문자로 시작하는 패턴이 있어야 함)
            val sentences = trimmed.split(Regex("[.!?]\\s+"))
            sentences.dropLast(1).forEach { sentence ->
                assertTrue(
                    sentence.isNotEmpty(),
                    "빈 문장이 포함되어서는 안 됩니다"
                )
            }
        }

        println("✅ [US4-AC1] 청크 개수: ${chunks.size}, 모든 청크가 문장 경계에서 분할됨")
    }

    @Test
    fun `US4-AC2 - 50자 짧은 텍스트는 단일 청크로 처리된다`() {
        // Given: 50자 미만의 짧은 텍스트
        val text = "이것은 매우 짧은 텍스트입니다. 청킹이 필요 없습니다."
        assertTrue(text.length < 100, "텍스트 길이가 100자 미만이어야 합니다")

        // When: FixedSizeChunkingService로 청킹
        val chunks = fixedSizeChunkingService.chunk(text)

        // Then: 단일 청크로 처리됨
        assertEquals(1, chunks.size, "짧은 텍스트는 단일 청크로 처리되어야 합니다")
        assertEquals(text.trim(), chunks[0].text, "청크 내용이 원본 텍스트와 일치해야 합니다")
        assertEquals(0, chunks[0].chunkIndex, "청크 인덱스는 0이어야 합니다")

        println("✅ [US4-AC2] 짧은 텍스트 (${text.length}자)가 단일 청크로 처리됨")
    }

    @Test
    fun `US4-AC3 - 5000자 긴 텍스트는 오버랩을 포함하여 분할된다`() {
        // Given: 5000자 이상의 긴 텍스트
        val text = buildString {
            repeat(100) { i ->
                append("문단 ${i}: ")
                append("이것은 충분히 긴 문장입니다. ".repeat(5))
                append("각 문단은 여러 문장으로 구성됩니다. ")
                append("청크 간 오버랩이 적용되어야 합니다. ")
                append("문맥의 연속성을 보장하기 위함입니다. ")
            }
        }
        assertTrue(text.length >= 5000, "텍스트 길이가 5000자 이상이어야 합니다 (실제: ${text.length})")

        // When: FixedSizeChunkingService로 청킹
        val chunks = fixedSizeChunkingService.chunk(text)

        // Then: 여러 청크로 분할됨
        assertTrue(chunks.size > 1, "긴 텍스트는 여러 청크로 분할되어야 합니다 (청크 수: ${chunks.size})")

        // 청크 간 오버랩 검증
        for (i in 0 until chunks.size - 1) {
            val currentChunk = chunks[i].text
            val nextChunk = chunks[i + 1].text

            // 겹치는 부분 찾기
            var overlapFound = false
            for (overlapLength in 10..100) {
                val currentSuffix = currentChunk.takeLast(overlapLength)
                if (nextChunk.contains(currentSuffix)) {
                    overlapFound = true
                    println("  청크 ${i}-${i+1} 오버랩: ${overlapLength}자")
                    break
                }
            }

            assertTrue(
                overlapFound,
                "청크 ${i}와 ${i+1} 사이에 오버랩이 있어야 합니다"
            )
        }

        // 청크 크기 일관성 검증
        chunks.forEach { chunk ->
            assertTrue(
                chunk.tokenCount > 0,
                "모든 청크는 토큰 수가 0보다 커야 합니다"
            )
        }

        println("✅ [US4-AC3] 긴 텍스트 (${text.length}자)가 ${chunks.size}개 청크로 분할됨, 오버랩 확인 완료")
    }
}
