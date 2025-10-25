package me.muheun.moaspace.service

import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.repository.VectorChunkRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * UniversalVectorIndexingService 통합 테스트
 *
 * 테스트 범위:
 * - indexEntity: 엔티티 인덱싱 (이벤트 발행 + 비동기 처리)
 * - reindexEntity: 재인덱싱 (기존 청크 삭제 + 새 청크 생성)
 * - deleteEntity: 청크 삭제
 * - search: 벡터 검색
 *
 * 주의: @Transactional 제거
 * - 비동기 처리와 @Transactional이 충돌하여 테스트가 실패할 수 있음
 * - 각 테스트 후 수동으로 데이터 정리
 */
@SpringBootTest
@DisplayName("UniversalVectorIndexingService 테스트")
class UniversalVectorIndexingServiceTest {

    @Autowired
    private lateinit var service: UniversalVectorIndexingService

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @AfterEach
    fun cleanup() {
        // 테스트 데이터 정리
        vectorChunkRepository.deleteAll()
    }

    @Test
    @DisplayName("indexEntity - 이벤트를 발행하고 즉시 CompletableFuture를 반환한다")
    fun `should publish event and return immediate CompletableFuture when indexing entity`() {
        // Given
        val request = VectorIndexRequest(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-123",
            fields = mapOf("name" to "스마트폰", "description" to "최신 스마트폰입니다"),
            metadata = mapOf("category" to "electronics")
        )

        // When
        val future = service.indexEntity(request)

        // Then
        // 1. 즉시 완료된 Future 반환 검증
        assertThat(future).isNotNull
        assertThat(future.isDone).isTrue()

        // 2. 비동기 처리 대기 (VectorProcessingService의 @Async @EventListener 처리 대기)
        Thread.sleep(1000)

        // 3. DB에 청크가 생성되었는지 검증
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "test_db",
            "products",
            "product-123"
        )

        assertThat(chunks).isNotEmpty
        assertThat(chunks.any { it.fieldName == "name" }).isTrue()
        assertThat(chunks.any { it.fieldName == "description" }).isTrue()
    }

    @Test
    @DisplayName("reindexEntity - 기존 청크를 삭제하고 새 청크를 생성한다")
    fun `should delete old chunks and create new chunks when reindexing`() {
        // Given: 먼저 청크 생성
        val initialRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-456",
            fields = mapOf("title" to "이전 제목", "content" to "이전 내용"),
            metadata = null
        )
        service.indexEntity(initialRequest)
        Thread.sleep(1000) // 비동기 처리 대기

        val oldChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "posts",
            "post-456"
        )
        assertThat(oldChunks).isNotEmpty

        // When: 재인덱싱
        val reindexRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-456",
            fields = mapOf("title" to "새 제목", "content" to "완전히 새로운 내용으로 업데이트되었습니다."),
            metadata = null
        )
        val future = service.reindexEntity(reindexRequest)
        Thread.sleep(1000) // 비동기 처리 대기

        // Then
        assertThat(future.isDone).isTrue()

        val newChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "posts",
            "post-456"
        )

        // 새 청크가 생성되었는지 검증
        assertThat(newChunks).isNotEmpty
        assertThat(newChunks.none { it.chunkText.contains("이전") }).isTrue()
        assertThat(newChunks.any { it.chunkText.contains("새로운") }).isTrue()
    }

    @Test
    @DisplayName("deleteEntity - 지정된 엔티티의 모든 청크를 삭제한다")
    fun `should delete all chunks of specified entity`() {
        // Given: 청크 생성
        val request = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "comments",
            recordKey = "comment-789",
            fields = mapOf("text" to "댓글 내용입니다."),
            metadata = null
        )
        service.indexEntity(request)
        Thread.sleep(1000) // 비동기 처리 대기

        val chunksBeforeDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "comments",
            "comment-789"
        )
        assertThat(chunksBeforeDelete).isNotEmpty

        // When: 삭제
        service.deleteEntity("vector_ai", "comments", "comment-789")

        // Then: 청크가 모두 삭제되었는지 검증
        val chunksAfterDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "comments",
            "comment-789"
        )
        assertThat(chunksAfterDelete).isEmpty()
    }

    @Test
    @DisplayName("search - 검색어와 유사한 청크를 찾아 반환한다")
    fun `should find and return similar chunks for query`() {
        // Given: 테스트 데이터 생성
        val product1 = VectorIndexRequest(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-1",
            fields = mapOf("name" to "갤럭시 스마트폰", "description" to "삼성의 최신 스마트폰입니다."),
            metadata = null
        )
        val product2 = VectorIndexRequest(
            namespace = "test_db",
            entity = "products",
            recordKey = "product-2",
            fields = mapOf("name" to "아이폰", "description" to "애플의 프리미엄 스마트폰입니다."),
            metadata = null
        )

        service.indexEntity(product1)
        service.indexEntity(product2)
        Thread.sleep(1500) // 비동기 처리 대기

        // When: 검색
        val searchRequest = VectorSearchRequest(
            query = "스마트폰 추천",
            namespace = "test_db",
            entity = "products",
            fieldName = null,
            limit = 10
        )
        val results = service.search(searchRequest)

        // Then
        assertThat(results).isNotEmpty
        assertThat(results.all { it.namespace == "test_db" }).isTrue()
        assertThat(results.all { it.entity == "products" }).isTrue()
        assertThat(results.first().similarityScore).isGreaterThan(0.0)
    }

    @Test
    @DisplayName("indexEntity - 여러 필드를 가진 엔티티도 정상 인덱싱된다")
    fun `should index entity with multiple fields successfully`() {
        // Given
        val request = VectorIndexRequest(
            namespace = "cms",
            entity = "articles",
            recordKey = "article-001",
            fields = mapOf(
                "title" to "기사 제목",
                "subtitle" to "부제목",
                "content" to "기사 본문 내용입니다. 여러 문장으로 구성되어 있습니다.",
                "summary" to "요약 내용입니다."
            ),
            metadata = mapOf(
                "author" to "홍길동",
                "category" to "tech"
            )
        )

        // When
        service.indexEntity(request)
        Thread.sleep(1000) // 비동기 처리 대기

        // Then: 각 필드별로 청크가 생성되었는지 검증
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "cms",
            "articles",
            "article-001"
        )

        assertThat(chunks).isNotEmpty()
        assertThat(chunks.map { chunk -> chunk.fieldName }.toSet()).containsAll(
            listOf("title", "subtitle", "content", "summary")
        )
    }

    @Test
    @DisplayName("deleteEntity - 다양한 namespace와 entity 조합으로 삭제 가능하다")
    fun `should delete entities from various namespace and entity combinations`() {
        // Given: 여러 엔티티 생성
        service.indexEntity(VectorIndexRequest("db1", "posts", "1", mapOf("text" to "내용1"), null))
        service.indexEntity(VectorIndexRequest("db2", "products", "2", mapOf("text" to "내용2"), null))
        service.indexEntity(VectorIndexRequest("cms", "articles", "article-123", mapOf("text" to "내용3"), null))
        Thread.sleep(2000) // 비동기 처리 대기

        // When & Then: 각각 삭제
        service.deleteEntity("db1", "posts", "1")
        assertThat(vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc("db1", "posts", "1")).isEmpty()

        service.deleteEntity("db2", "products", "2")
        assertThat(vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc("db2", "products", "2")).isEmpty()

        service.deleteEntity("cms", "articles", "article-123")
        assertThat(vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc("cms", "articles", "article-123")).isEmpty()
    }

    // ========================================
    // User Story 1: 다양한 엔티티에 벡터 검색 추가
    // ========================================

    @Test
    @DisplayName("[US1] Scenario 1 - Product 테이블 10개 레코드, 스마트폰 검색 시 유사도 순 반환")
    fun `US1_Scenario1 should return products sorted by similarity when searching for smartphone`() {
        // Given: 10개의 Product 레코드 생성
        val products = listOf(
            "갤럭시 스마트폰 최신 모델입니다",
            "아이폰 스마트폰 프리미엄 제품입니다",
            "노트북 컴퓨터 고성능 제품입니다",
            "태블릿 PC 휴대용 기기입니다",
            "스마트워치 웨어러블 디바이스입니다",
            "무선 이어폰 블루투스 제품입니다",
            "스마트폰 케이스 액세서리입니다",
            "스마트폰 충전기 고속 충전 지원합니다",
            "게이밍 마우스 RGB 조명 지원합니다",
            "키보드 기계식 스위치 제품입니다"
        )

        products.forEachIndexed { index, description ->
            service.indexEntity(
                VectorIndexRequest(
                    namespace = "shop_db",
                    entity = "products",
                    recordKey = "product-${index + 1}",
                    fields = mapOf(
                        "name" to "제품 ${index + 1}",
                        "description" to description
                    ),
                    metadata = null
                )
            )
        }
        Thread.sleep(3000) // 비동기 처리 대기 (10개 제품)

        // When: "스마트폰" 검색
        val searchRequest = VectorSearchRequest(
            query = "스마트폰",
            namespace = "shop_db",
            entity = "products",
            fieldName = null,
            limit = 10
        )
        val results = service.search(searchRequest)

        // Then: 결과가 유사도 순으로 정렬되어 반환됨
        assertThat(results).isNotEmpty
        assertThat(results.size).isLessThanOrEqualTo(10)

        // 유사도 점수가 내림차순으로 정렬되었는지 확인
        for (i in 0 until results.size - 1) {
            assertThat(results[i].similarityScore).isGreaterThanOrEqualTo(results[i + 1].similarityScore)
        }

        // 모든 결과가 products 엔티티에서만 나왔는지 확인
        assertThat(results.all { it.entity == "products" }).isTrue()
        assertThat(results.all { it.namespace == "shop_db" }).isTrue()
    }

    @Test
    @DisplayName("[US1] Scenario 2 - posts와 products 모두 인덱싱, posts만 검색 시 products 제외")
    fun `US1_Scenario2 should return only posts when searching in posts entity`() {
        // Given: posts와 products 모두 인덱싱
        // Posts 데이터
        service.indexEntity(
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "posts",
                recordKey = "post-1",
                fields = mapOf(
                    "title" to "Spring Boot 가이드",
                    "content" to "Spring Boot 프레임워크 사용법"
                ),
                metadata = null
            )
        )
        service.indexEntity(
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "posts",
                recordKey = "post-2",
                fields = mapOf(
                    "title" to "Kotlin 튜토리얼",
                    "content" to "Kotlin 언어 학습 자료"
                ),
                metadata = null
            )
        )

        // Products 데이터
        service.indexEntity(
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "products",
                recordKey = "product-1",
                fields = mapOf(
                    "name" to "Spring Boot 책",
                    "description" to "Spring Boot 학습 도서"
                ),
                metadata = null
            )
        )

        Thread.sleep(2000) // 비동기 처리 대기

        // When: posts 엔티티만 검색
        val searchRequest = VectorSearchRequest(
            query = "Spring Boot",
            namespace = "vector_ai",
            entity = "posts",
            fieldName = null,
            limit = 10
        )
        val results = service.search(searchRequest)

        // Then: posts 결과만 반환되고 products는 제외됨
        assertThat(results).isNotEmpty
        assertThat(results.all { it.entity == "posts" }).isTrue()
        assertThat(results.none { it.entity == "products" }).isTrue()
        assertThat(results.all { it.namespace == "vector_ai" }).isTrue()
    }

    @Test
    @DisplayName("[US1] Scenario 3 - comments 테이블 추가 시 코드 변경 없이 즉시 인덱싱 및 검색")
    fun `US1_Scenario3 should index and search comments without code changes`() {
        // Given: 새로운 comments 엔티티 추가 (코드 변경 없이)
        service.indexEntity(
            VectorIndexRequest(
                namespace = "forum_db",
                entity = "comments",
                recordKey = "comment-1",
                fields = mapOf("content" to "이 게시글 정말 유익하네요!"),
                metadata = mapOf("author" to "user1")
            )
        )
        service.indexEntity(
            VectorIndexRequest(
                namespace = "forum_db",
                entity = "comments",
                recordKey = "comment-2",
                fields = mapOf("content" to "좋은 정보 감사합니다."),
                metadata = mapOf("author" to "user2")
            )
        )
        service.indexEntity(
            VectorIndexRequest(
                namespace = "forum_db",
                entity = "comments",
                recordKey = "comment-3",
                fields = mapOf("content" to "도움이 많이 되었습니다."),
                metadata = mapOf("author" to "user3")
            )
        )

        Thread.sleep(2000) // 비동기 처리 대기

        // When: comments 검색
        val searchRequest = VectorSearchRequest(
            query = "유익한 정보",
            namespace = "forum_db",
            entity = "comments",
            fieldName = null,
            limit = 5
        )
        val results = service.search(searchRequest)

        // Then: comments가 정상적으로 인덱싱되고 검색됨
        assertThat(results).isNotEmpty
        assertThat(results.all { it.entity == "comments" }).isTrue()
        assertThat(results.all { it.namespace == "forum_db" }).isTrue()

        // 메타데이터도 정상적으로 저장되었는지 확인
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "forum_db",
            "comments",
            "comment-1"
        )
        assertThat(chunks).isNotEmpty
        assertThat(chunks.first().metadata).isNotNull
        assertThat(chunks.first().metadata!!["author"]).isEqualTo("user1")
    }
}
