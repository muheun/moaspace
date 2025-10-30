package me.muheun.moaspace.integration

import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.service.UniversalVectorIndexingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.util.concurrent.TimeUnit

/**
 * 메타데이터 필터링 통합 테스트
 *
 * User Story 3의 Acceptance Scenarios를 검증합니다:
 * 1. namespace/entity 필터링으로 검색 범위 제한
 * 2. fieldName 필터링으로 특정 필드만 검색
 * 3. 검색 결과에 메타데이터 포함 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class MetadataFilteringIntegrationTest {

    @Autowired
    private lateinit var universalVectorIndexingService: UniversalVectorIndexingService

    @BeforeEach
    fun setup() {
        // 비동기 작업 완료 대기
        Thread.sleep(500)
    }

    @Test
    @DisplayName("시나리오 1: posts와 products 모두 인덱싱되었을 때, entity='posts'로 검색하면 posts 결과만 반환")
    fun testEntityFiltering() {
        // Given: posts와 products 인덱싱
        val post1Request = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-1",
            fields = mapOf(
                "title" to "Spring Boot 가이드",
                "content" to "Spring Boot는 Java 기반의 강력한 프레임워크입니다."
            )
        )

        val post2Request = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-2",
            fields = mapOf(
                "title" to "Kotlin 튜토리얼",
                "content" to "Kotlin은 현대적인 JVM 언어입니다."
            )
        )

        val productRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "products",
            recordKey = "product-1",
            fields = mapOf(
                "name" to "Spring Boot 서적",
                "description" to "Spring Boot를 배우기 위한 최고의 책입니다."
            )
        )

        // 인덱싱 실행
        universalVectorIndexingService.indexEntity(post1Request).get(10, TimeUnit.SECONDS)
        universalVectorIndexingService.indexEntity(post2Request).get(10, TimeUnit.SECONDS)
        universalVectorIndexingService.indexEntity(productRequest).get(10, TimeUnit.SECONDS)

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: "Spring Boot"로 posts만 검색
        val searchRequest = VectorSearchRequest(
            query = "Spring Boot",
            namespace = "vector_ai",
            entity = "posts",
            limit = 10
        )

        val results = universalVectorIndexingService.search(searchRequest)

        // Then: posts 결과만 반환되어야 함
        assertThat(results).isNotEmpty
        assertThat(results).allMatch { it.entity == "posts" }
        assertThat(results).noneMatch { it.entity == "products" }

        // 메타데이터 확인
        assertThat(results).allMatch { it.namespace == "vector_ai" }
        assertThat(results.first().recordKey).isIn("post-1", "post-2")
    }

    @Test
    @DisplayName("시나리오 2: title과 content 인덱싱되었을 때, fieldName='title'로 검색하면 title 필드만 반환")
    fun testFieldNameFiltering() {
        // Given: 게시글 인덱싱 (title과 content 모두)
        val postRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-100",
            fields = mapOf(
                "title" to "PostgreSQL 데이터베이스 가이드",
                "content" to "MySQL은 가장 인기 있는 관계형 데이터베이스입니다. 하지만 PostgreSQL도 훌륭한 선택입니다."
            )
        )

        universalVectorIndexingService.indexEntity(postRequest).get(10, TimeUnit.SECONDS)

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: "PostgreSQL"로 title 필드만 검색
        val titleSearchRequest = VectorSearchRequest(
            query = "PostgreSQL",
            namespace = "vector_ai",
            entity = "posts",
            fieldName = "title",
            limit = 10
        )

        val titleResults = universalVectorIndexingService.search(titleSearchRequest)

        // Then: title 필드 결과만 반환
        assertThat(titleResults).isNotEmpty
        assertThat(titleResults).allMatch { it.fieldName == "title" }
        assertThat(titleResults.first().chunkText).contains("PostgreSQL")

        // When: "MySQL"로 content 필드만 검색
        val contentSearchRequest = VectorSearchRequest(
            query = "MySQL",
            namespace = "vector_ai",
            entity = "posts",
            fieldName = "content",
            limit = 10
        )

        val contentResults = universalVectorIndexingService.search(contentSearchRequest)

        // Then: content 필드 결과만 반환
        assertThat(contentResults).isNotEmpty
        assertThat(contentResults).allMatch { it.fieldName == "content" }
        assertThat(contentResults.first().chunkText).contains("MySQL")
    }

    @Test
    @DisplayName("시나리오 3: 검색 결과에 namespace, entity, fieldName 메타데이터 포함 확인")
    fun testMetadataInSearchResults() {
        // Given: products 인덱싱
        val productRequest = VectorIndexRequest(
            namespace = "ecommerce",
            entity = "products",
            recordKey = "product-200",
            fields = mapOf(
                "name" to "스마트폰 갤럭시 S24",
                "description" to "최신 안드로이드 스마트폰으로 뛰어난 성능을 자랑합니다."
            ),
            metadata = mapOf(
                "category" to "electronics",
                "price" to 1200000
            )
        )

        universalVectorIndexingService.indexEntity(productRequest).get(10, TimeUnit.SECONDS)

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: "스마트폰" 검색
        val searchRequest = VectorSearchRequest(
            query = "스마트폰",
            namespace = "ecommerce",
            entity = "products",
            limit = 10
        )

        val results = universalVectorIndexingService.search(searchRequest)

        // Then: 메타데이터가 정확하게 포함되어야 함
        assertThat(results).isNotEmpty

        val firstResult = results.first()
        assertThat(firstResult.namespace).isEqualTo("ecommerce")
        assertThat(firstResult.entity).isEqualTo("products")
        assertThat(firstResult.recordKey).isEqualTo("product-200")
        assertThat(firstResult.fieldName).isIn("name", "description")
        assertThat(firstResult.chunkText).isNotBlank
        assertThat(firstResult.similarityScore).isGreaterThan(0.0)

        // 추가 메타데이터 확인
        assertThat(firstResult.metadata).isNotNull
        assertThat(firstResult.metadata).containsKey("category")
        assertThat(firstResult.metadata).containsEntry("category", "electronics")
    }

    @Test
    @DisplayName("시나리오 4: 여러 namespace가 존재할 때, 특정 namespace로 검색 범위 제한")
    fun testNamespaceFiltering() {
        // Given: 여러 namespace에 데이터 인덱싱
        val vectorAiRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "post-1",
            fields = mapOf("title" to "벡터 검색 시스템 구축하기")
        )

        val ecommerceRequest = VectorIndexRequest(
            namespace = "ecommerce",
            entity = "posts",
            recordKey = "post-2",
            fields = mapOf("title" to "벡터 검색으로 추천 시스템 개선")
        )

        universalVectorIndexingService.indexEntity(vectorAiRequest).get(10, TimeUnit.SECONDS)
        universalVectorIndexingService.indexEntity(ecommerceRequest).get(10, TimeUnit.SECONDS)

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: "벡터 검색"으로 vector_ai namespace만 검색
        val searchRequest = VectorSearchRequest(
            query = "벡터 검색",
            namespace = "vector_ai",
            entity = "posts",
            limit = 10
        )

        val results = universalVectorIndexingService.search(searchRequest)

        // Then: vector_ai namespace 결과만 반환
        assertThat(results).isNotEmpty
        assertThat(results).allMatch { it.namespace == "vector_ai" }
        assertThat(results).noneMatch { it.namespace == "ecommerce" }
        assertThat(results.first().recordKey).isEqualTo("post-1")
    }

    @Test
    @DisplayName("시나리오 5: 필터 없이 전체 검색 시 모든 namespace/entity 결과 반환")
    fun testSearchWithoutFilters() {
        // Given: 여러 namespace/entity에 데이터 인덱싱
        val requests = listOf(
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "posts",
                recordKey = "post-1",
                fields = mapOf("content" to "인공지능 기술의 발전")
            ),
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "comments",
                recordKey = "comment-1",
                fields = mapOf("text" to "AI는 정말 놀라운 기술입니다")
            ),
            VectorIndexRequest(
                namespace = "ecommerce",
                entity = "products",
                recordKey = "product-1",
                fields = mapOf("name" to "AI 스피커")
            )
        )

        requests.forEach { request ->
            universalVectorIndexingService.indexEntity(request).get(10, TimeUnit.SECONDS)
        }

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: "AI" 검색 (필터 없음)
        val searchRequest = VectorSearchRequest(
            query = "AI",
            namespace = null, // 전체 namespace
            entity = null, // 전체 entity
            limit = 10
        )

        val results = universalVectorIndexingService.search(searchRequest)

        // Then: 여러 namespace/entity에서 결과 반환
        assertThat(results).hasSizeGreaterThanOrEqualTo(3)

        val namespaces = results.map { it.namespace }.distinct()
        val entities = results.map { it.entity }.distinct()

        assertThat(namespaces).contains("vector_ai", "ecommerce")
        assertThat(entities).contains("posts", "comments", "products")
    }

    @Test
    @DisplayName("시나리오 6: 필드별 가중치 검색 시 메타데이터 정확성 확인")
    fun testWeightedSearchMetadata() {
        // Given: title과 content가 있는 게시글 인덱싱
        val postRequest = VectorIndexRequest(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = "weighted-post",
            fields = mapOf(
                "title" to "Kotlin Coroutines 가이드",
                "content" to "Java의 CompletableFuture보다 Kotlin Coroutines가 더 직관적입니다."
            )
        )

        universalVectorIndexingService.indexEntity(postRequest).get(10, TimeUnit.SECONDS)

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // When: title 가중치 70%, content 가중치 30%로 "Kotlin" 검색
        val searchRequest = VectorSearchRequest(
            query = "Kotlin",
            namespace = "vector_ai",
            entity = "posts",
            fieldWeights = mapOf(
                "title" to 0.7,
                "content" to 0.3
            ),
            limit = 10
        )

        val results = universalVectorIndexingService.search(searchRequest)

        // Then: 결과에 메타데이터 포함 확인
        assertThat(results).isNotEmpty

        results.forEach { result ->
            assertThat(result.namespace).isEqualTo("vector_ai")
            assertThat(result.entity).isEqualTo("posts")
            assertThat(result.recordKey).isEqualTo("weighted-post")
            assertThat(result.fieldName).isIn("title", "content")
            assertThat(result.chunkText).isNotBlank
        }
    }
}
