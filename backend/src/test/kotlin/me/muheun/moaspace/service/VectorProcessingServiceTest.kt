package me.muheun.moaspace.service

import me.muheun.moaspace.event.VectorIndexingRequestedEvent
import me.muheun.moaspace.repository.VectorChunkRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * VectorProcessingService 통합 테스트
 *
 * 테스트 범위:
 * - handleVectorIndexingRequest: 이벤트 수신 및 백그라운드 벡터 처리 검증
 * - 실제 PostgreSQL + pgvector 연동 E2E 테스트
 *
 * 주의:
 * - 비동기 처리(@Async @EventListener)를 위해 Thread.sleep() 사용
 * - 각 테스트 후 데이터 정리 필수
 */
@SpringBootTest
@DisplayName("VectorProcessingService 통합 테스트")
class VectorProcessingServiceTest {

    @Autowired
    private lateinit var vectorProcessingService: VectorProcessingService

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @AfterEach
    fun cleanup() {
        vectorChunkRepository.deleteAll()
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 이벤트를 수신하고 필드별로 벡터화를 처리한다")
    fun `should process event and vectorize each field`() {
        // Given
        val event = VectorIndexingRequestedEvent(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-123",
            fields = mapOf(
                "name" to "스마트폰",
                "description" to "최신 기술이 적용된 스마트폰입니다."
            ),
            metadata = mapOf("category" to "electronics")
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1500) // 비동기 처리 대기

        // Then: DB에 청크가 생성되었는지 검증
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "products",
            "product-123"
        )

        assertThat(chunks).isNotEmpty
        assertThat(chunks.any { it.fieldName == "name" }).isTrue()
        assertThat(chunks.any { it.fieldName == "description" }).isTrue()
        assertThat(chunks.all { it.namespace == "test_db" }).isTrue()
        assertThat(chunks.all { it.entity == "products" }).isTrue()
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 빈 필드 맵도 처리하며 에러가 발생하지 않는다")
    fun `should handle empty fields map without error`() {
        // Given
        val event = VectorIndexingRequestedEvent(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-empty",
            fields = emptyMap(),
            metadata = null
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1000) // 비동기 처리 대기

        // Then: 청크가 생성되지 않아야 함
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "products",
            "product-empty"
        )
        assertThat(chunks).isEmpty()
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 여러 청크가 생성되는 긴 텍스트도 정상 처리한다")
    fun `should process long text with multiple chunks`() {
        // Given
        val longText = """
            첫 번째 문장입니다. 두 번째 문장입니다. 세 번째 문장입니다.
            네 번째 문장입니다. 다섯 번째 문장입니다.
        """.trimIndent()

        val event = VectorIndexingRequestedEvent(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-456",
            fields = mapOf("content" to longText),
            metadata = null
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1500) // 비동기 처리 대기

        // Then
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "posts",
            "post-456"
        )

        assertThat(chunks).isNotEmpty
        assertThat(chunks.all { it.fieldName == "content" }).isTrue()
        assertThat(chunks.first().chunkVector).isNotNull
        assertThat(chunks.first().chunkVector!!.toArray().size).isEqualTo(1536) // 벡터 차원 검증
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 메타데이터가 VectorChunk에 정확히 저장된다")
    fun `should store metadata in VectorChunk correctly`() {
        // Given
        val metadata = mapOf(
            "author" to "홍길동",
            "category" to "tech",
            "tags" to listOf("AI", "ML")
        )
        val event = VectorIndexingRequestedEvent(
            namespace = "cms",
            entity = "articles",
            recordKey = "article-789",
            fields = mapOf("title" to "기사 제목"),
            metadata = metadata
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1000) // 비동기 처리 대기

        // Then
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "cms",
            "articles",
            "article-789"
        )

        assertThat(chunks).isNotEmpty
        assertThat(chunks.first().metadata).isEqualTo(metadata)
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 여러 필드를 처리하며 모든 필드가 저장된다")
    fun `should process multiple fields and save all chunks`() {
        // Given
        val event = VectorIndexingRequestedEvent(
            namespace = "cms",
            entity = "articles",
            recordKey = "article-multi",
            fields = mapOf(
                "title" to "제목",
                "subtitle" to "부제목",
                "content" to "본문 내용",
                "summary" to "요약"
            ),
            metadata = null
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(2000) // 비동기 처리 대기 (4개 필드이므로 더 길게)

        // Then
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "cms",
            "articles",
            "article-multi"
        )

        assertThat(chunks).isNotEmpty
        val fieldNames = chunks.map { it.fieldName }.toSet()
        assertThat(fieldNames).containsAll(listOf("title", "subtitle", "content", "summary"))
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - VectorChunk의 모든 필드가 올바르게 설정된다")
    fun `should set all VectorChunk fields correctly`() {
        // Given
        val event = VectorIndexingRequestedEvent(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-complete",
            fields = mapOf("title" to "완전한 제목"),
            metadata = mapOf("version" to 1)
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1000) // 비동기 처리 대기

        // Then
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "posts",
            "post-complete"
        )

        assertThat(chunks).isNotEmpty
        val chunk = chunks.first()

        assertThat(chunk.namespace).isEqualTo("vector_ai")
        assertThat(chunk.entity).isEqualTo("posts")
        assertThat(chunk.recordKey).isEqualTo("post-complete")
        assertThat(chunk.fieldName).isEqualTo("title")
        assertThat(chunk.chunkText).isEqualTo("완전한 제목")
        assertThat(chunk.chunkVector).isNotNull
        assertThat(chunk.chunkVector!!.toArray().size).isEqualTo(1536)
        assertThat(chunk.chunkIndex).isEqualTo(0)
        assertThat(chunk.startPosition).isEqualTo(0)
        assertThat(chunk.endPosition).isGreaterThan(0)
        assertThat(chunk.metadata).isEqualTo(mapOf("version" to 1))
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 청크 인덱스가 순차적으로 증가한다")
    fun `should have sequential chunk indices`() {
        // Given: 여러 청크가 생성되도록 긴 텍스트
        val longText = """
            이것은 긴 문서의 첫 번째 문단입니다. 여러 문장으로 구성되어 있습니다.
            두 번째 문단입니다. 청킹 알고리즘이 이를 여러 청크로 분할할 것입니다.
            세 번째 문단도 추가합니다. 충분한 길이를 확보하기 위함입니다.
        """.trimIndent()

        val event = VectorIndexingRequestedEvent(
            namespace = "test_db",
            entity = "docs",
            recordKey = "doc-123",
            fields = mapOf("content" to longText),
            metadata = null
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event)
        Thread.sleep(1500) // 비동기 처리 대기

        // Then
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "docs",
            "doc-123"
        )

        assertThat(chunks).isNotEmpty

        // 청크 인덱스가 0부터 순차적으로 증가하는지 검증
        chunks.forEachIndexed { index, chunk ->
            assertThat(chunk.chunkIndex).isEqualTo(index)
        }
    }

    @Test
    @DisplayName("handleVectorIndexingRequest - 같은 entity의 다른 레코드는 독립적으로 처리된다")
    fun `should process different records of same entity independently`() {
        // Given
        val event1 = VectorIndexingRequestedEvent(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-1",
            fields = mapOf("name" to "제품 1"),
            metadata = null
        )
        val event2 = VectorIndexingRequestedEvent(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-2",
            fields = mapOf("name" to "제품 2"),
            metadata = null
        )

        // When
        vectorProcessingService.handleVectorIndexingRequest(event1)
        vectorProcessingService.handleVectorIndexingRequest(event2)
        Thread.sleep(2000) // 비동기 처리 대기

        // Then
        val chunks1 = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "products",
            "product-1"
        )
        val chunks2 = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "products",
            "product-2"
        )

        assertThat(chunks1).isNotEmpty
        assertThat(chunks2).isNotEmpty
        assertThat(chunks1.all { it.recordKey == "product-1" }).isTrue()
        assertThat(chunks2.all { it.recordKey == "product-2" }).isTrue()
    }
}
