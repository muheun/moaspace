package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorChunk
import me.muheun.moaspace.service.VectorService
import com.pgvector.PGvector
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

/**
 * VectorChunkRepository 테스트
 *
 * 범용 벡터 청크 CRUD 및 검색 기능을 검증합니다.
 * 모든 테스트는 실제 PostgreSQL + pgvector 환경에서 실행됩니다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VectorChunkRepository 테스트")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(VectorService::class)
class VectorChunkRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @Autowired
    private lateinit var vectorService: VectorService

    @AfterEach
    fun cleanup() {
        vectorChunkRepository.deleteAll()
        entityManager.clear()
    }

    // ========================================
    // 범용 CRUD 메서드 테스트
    // ========================================

    @Test
    @DisplayName("namespace+entity+recordKey로 청크를 조회할 수 있다")
    fun `should find chunks by namespace and entity and recordKey`() {
        // Given: 여러 청크 생성
        val chunk1 = createTestChunk("vector_ai", "posts", "post-1", "title", 0, "제목 1")
        val chunk2 = createTestChunk("vector_ai", "posts", "post-1", "content", 0, "내용 1")
        val chunk3 = createTestChunk("vector_ai", "posts", "post-2", "title", 0, "제목 2")

        entityManager.persist(chunk1)
        entityManager.persist(chunk2)
        entityManager.persist(chunk3)
        entityManager.flush()

        // When: post-1의 모든 청크 조회
        val results = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai",
            "posts",
            "post-1"
        )

        // Then: post-1의 청크만 반환
        assertThat(results).hasSize(2)
        assertThat(results.map { it.fieldName }).containsExactlyInAnyOrder("title", "content")
        assertThat(results.all { it.recordKey == "post-1" }).isTrue()
    }

    @Test
    @DisplayName("namespace+entity+recordKey+fieldName으로 특정 필드 청크만 조회할 수 있다")
    fun `should find chunks by namespace and entity and recordKey and fieldName`() {
        // Given
        val chunk1 = createTestChunk("cms", "articles", "article-1", "title", 0, "제목")
        val chunk2 = createTestChunk("cms", "articles", "article-1", "content", 0, "내용 1")
        val chunk3 = createTestChunk("cms", "articles", "article-1", "content", 1, "내용 2")

        entityManager.persist(chunk1)
        entityManager.persist(chunk2)
        entityManager.persist(chunk3)
        entityManager.flush()

        // When: content 필드만 조회
        val results = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyAndFieldNameOrderByChunkIndexAsc(
            "cms",
            "articles",
            "article-1",
            "content"
        )

        // Then: content 필드 청크만 반환
        assertThat(results).hasSize(2)
        assertThat(results.all { it.fieldName == "content" }).isTrue()
        assertThat(results.map { it.chunkIndex }).containsExactly(0, 1)
    }

    @Test
    @DisplayName("특정 레코드의 모든 청크를 삭제할 수 있다")
    fun `should delete all chunks of a record`() {
        // Given
        val chunk1 = createTestChunk("test_db", "products", "product-1", "name", 0, "제품명")
        val chunk2 = createTestChunk("test_db", "products", "product-1", "description", 0, "설명")
        val chunk3 = createTestChunk("test_db", "products", "product-2", "name", 0, "제품명2")

        entityManager.persist(chunk1)
        entityManager.persist(chunk2)
        entityManager.persist(chunk3)
        entityManager.flush()
        entityManager.clear()

        // When: product-1 청크 삭제
        vectorChunkRepository.deleteByNamespaceAndEntityAndRecordKey(
            "test_db",
            "products",
            "product-1"
        )
        entityManager.flush()
        entityManager.clear()

        // Then: product-1 청크는 삭제되고 product-2는 남아있음
        val remainingChunks = vectorChunkRepository.findAll()
        assertThat(remainingChunks).hasSize(1)
        assertThat(remainingChunks.first().recordKey).isEqualTo("product-2")
    }

    @Test
    @DisplayName("특정 레코드의 특정 필드 청크만 삭제할 수 있다")
    fun `should delete chunks of specific field`() {
        // Given
        val chunk1 = createTestChunk("app_db", "users", "user-1", "name", 0, "이름")
        val chunk2 = createTestChunk("app_db", "users", "user-1", "bio", 0, "자기소개")

        entityManager.persist(chunk1)
        entityManager.persist(chunk2)
        entityManager.flush()
        entityManager.clear()

        // When: bio 필드만 삭제
        vectorChunkRepository.deleteByNamespaceAndEntityAndRecordKeyAndFieldName(
            "app_db",
            "users",
            "user-1",
            "bio"
        )
        entityManager.flush()
        entityManager.clear()

        // Then: name 필드는 남아있고 bio는 삭제됨
        val remainingChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "app_db",
            "users",
            "user-1"
        )
        assertThat(remainingChunks).hasSize(1)
        assertThat(remainingChunks.first().fieldName).isEqualTo("name")
    }

    // ========================================
    // 벡터 유사도 검색 테스트
    // ========================================

    @Test
    @DisplayName("벡터 유사도 검색 - namespace와 entity로 필터링")
    fun `should search by vector similarity with namespace and entity filter`() {
        // Given: posts와 products 데이터 생성
        val postChunk = createTestChunk("vector_ai", "posts", "post-1", "content", 0, "Spring Boot")
        val productChunk = createTestChunk("vector_ai", "products", "product-1", "name", 0, "Spring Boot 책")

        entityManager.persist(postChunk)
        entityManager.persist(productChunk)
        entityManager.flush()

        // When: posts 엔티티만 검색
        val queryVector = vectorService.generateEmbedding("Spring Boot").toString()
        val results = vectorChunkRepository.findSimilarRecords(
            queryVector,
            "vector_ai",
            "posts",
            10
        )

        // Then: posts 결과만 반환
        assertThat(results).hasSize(1)
        assertThat(results.first().getRecordKey()).isEqualTo("post-1")
    }

    @Test
    @DisplayName("벡터 유사도 검색 - 유사도 점수 내림차순 정렬")
    fun `should return results sorted by similarity score desc`() {
        // Given: 여러 제품 생성
        val products = listOf(
            "갤럭시 스마트폰" to "product-1",
            "아이폰 스마트폰" to "product-2",
            "노트북 컴퓨터" to "product-3",
            "스마트폰 케이스" to "product-4"
        )

        products.forEach { (description, key) ->
            val chunk = createTestChunk("shop_db", "products", key, "description", 0, description)
            entityManager.persist(chunk)
        }
        entityManager.flush()

        // When: "스마트폰" 검색
        val queryVector = vectorService.generateEmbedding("스마트폰").toString()
        val results = vectorChunkRepository.findSimilarRecords(
            queryVector,
            "shop_db",
            "products",
            10
        )

        // Then: 유사도 점수가 내림차순으로 정렬됨
        assertThat(results).isNotEmpty
        for (i in 0 until results.size - 1) {
            assertThat(results[i].getScore()).isGreaterThanOrEqualTo(results[i + 1].getScore())
        }
    }

    @Test
    @DisplayName("청크 상세 검색 - 각 레코드별 최고 점수 청크 1개씩 반환")
    fun `should return top chunk per record with details`() {
        // Given: 한 레코드에 여러 청크
        val chunk1 = createTestChunk("cms", "posts", "post-1", "content", 0, "Kotlin 프로그래밍")
        val chunk2 = createTestChunk("cms", "posts", "post-1", "content", 1, "Java 프로그래밍")
        val chunk3 = createTestChunk("cms", "posts", "post-2", "content", 0, "Python 프로그래밍")

        entityManager.persist(chunk1)
        entityManager.persist(chunk2)
        entityManager.persist(chunk3)
        entityManager.flush()

        // When: "Kotlin" 검색
        val queryVector = vectorService.generateEmbedding("Kotlin").toString()
        val results = vectorChunkRepository.findTopChunksByRecord(
            queryVector,
            "cms",
            "posts",
            null,
            10
        )

        // Then: 각 레코드당 1개씩만 반환
        assertThat(results).hasSize(2)
        assertThat(results.map { it.getRecordKey() }).containsExactlyInAnyOrder("post-1", "post-2")
    }

    @Test
    @DisplayName("청크 상세 검색 - fieldName 필터링")
    fun `should search with fieldName filter`() {
        // Given: title과 content 필드
        val titleChunk = createTestChunk("blog", "posts", "post-1", "title", 0, "Spring Boot 가이드")
        val contentChunk = createTestChunk("blog", "posts", "post-1", "content", 0, "Kotlin으로 작성된 가이드")

        entityManager.persist(titleChunk)
        entityManager.persist(contentChunk)
        entityManager.flush()

        // When: title 필드만 검색
        val queryVector = vectorService.generateEmbedding("Spring Boot").toString()
        val results = vectorChunkRepository.findTopChunksByRecord(
            queryVector,
            "blog",
            "posts",
            "title",
            10
        )

        // Then: title 필드 결과만 반환
        assertThat(results).hasSize(1)
        assertThat(results.first().getFieldName()).isEqualTo("title")
    }

    @Test
    @DisplayName("필드별 가중치 검색 - title 60%, content 40%")
    fun `should search with field weights`() {
        // Given: title과 content가 있는 게시글
        val titleChunk = createTestChunk("vector_ai", "posts", "post-1", "title", 0, "PostgreSQL 데이터베이스")
        val contentChunk = createTestChunk("vector_ai", "posts", "post-1", "content", 0, "MySQL 설명 내용")

        entityManager.persist(titleChunk)
        entityManager.persist(contentChunk)
        entityManager.flush()

        // When: "PostgreSQL" 검색 (title 60%, content 40%)
        val queryVector = vectorService.generateEmbedding("PostgreSQL").toString()
        val results = vectorChunkRepository.findByWeightedFieldScore(
            queryVector,
            "vector_ai",
            "posts",
            titleWeight = 0.6,
            contentWeight = 0.4,
            10
        )

        // Then: 가중치가 적용된 점수로 정렬됨
        assertThat(results).isNotEmpty
        assertThat(results.first().getRecordKey()).isEqualTo("post-1")
        assertThat(results.first().getWeightedScore()).isGreaterThan(0.0)
    }

    // ========================================
    // 복합 인덱스 성능 테스트
    // ========================================

    @Test
    @DisplayName("복합 인덱스 - namespace+entity+recordKey 조합으로 빠른 조회")
    fun `should quickly find chunks using composite index`() {
        // Given: 많은 청크 생성
        (1..100).forEach { i ->
            val chunk = createTestChunk("perf_test", "data", "record-$i", "field", 0, "Data $i")
            entityManager.persist(chunk)
            if (i % 20 == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }
        entityManager.flush()

        // When: 특정 레코드 조회 (복합 인덱스 사용)
        val startTime = System.currentTimeMillis()
        val results = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "perf_test",
            "data",
            "record-50"
        )
        val elapsedTime = System.currentTimeMillis() - startTime

        // Then: 빠르게 조회되어야 함 (< 100ms)
        assertThat(results).hasSize(1)
        assertThat(elapsedTime).isLessThan(100)
    }

    // ========================================
    // Helper Methods
    // ========================================

    private fun createTestChunk(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String,
        chunkIndex: Int,
        text: String
    ): VectorChunk {
        val vector = vectorService.generateEmbedding(text)
        return VectorChunk(
            namespace = namespace,
            entity = entity,
            recordKey = recordKey,
            fieldName = fieldName,
            chunkIndex = chunkIndex,
            chunkText = text,
            chunkVector = vector,
            startPosition = 0,
            endPosition = text.length,
            metadata = null
        )
    }
}
