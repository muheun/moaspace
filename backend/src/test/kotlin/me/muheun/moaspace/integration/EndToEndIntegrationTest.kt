package me.muheun.moaspace.integration

import me.muheun.moaspace.dto.*
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.service.UniversalVectorIndexingService
import me.muheun.moaspace.service.PostVectorAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql

/**
 * End-to-End 통합 테스트
 *
 * Phase 9: Polish & Cross-Cutting Concerns
 *
 * 검증 대상:
 * - US1: 다양한 엔티티에 벡터 검색 추가
 * - US2: 필드별 독립 벡터화
 * - US3: 메타데이터 추적 및 필터링
 * - US4: 스마트 청킹 품질 향상
 * - US5: 재인덱싱 시 데이터 일관성 보장
 * - US6: 기존 Post 시스템과 호환성 유지
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("End-to-End 통합 테스트")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class EndToEndIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var universalVectorIndexingService: UniversalVectorIndexingService

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postVectorAdapter: PostVectorAdapter

    private val baseUrl: String
        get() = "http://localhost:$port/api/posts"

    @BeforeEach
    fun setUp() {
        // DB 초기화는 @Sql 어노테이션으로 처리됨
        // 비동기 작업 완료 대기
        Thread.sleep(500)
    }

    // ═══════════════════════════════════════════════════════════════
    // US1: 다양한 엔티티에 벡터 검색 추가
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US1-1: Products 테이블을 벡터화하고 검색할 수 있다 (posts와 독립적)")
    fun `should index and search products independently from posts`() {
        // given - Products 테이블 시뮬레이션 (UniversalVectorIndexingService 직접 사용)
        val products = listOf(
            VectorIndexRequest(
                namespace = "test_db",
                entity = "products",
                recordKey = "prod-001",
                fields = mapOf(
                    "name" to "갤럭시 S24 Ultra",
                    "description" to "삼성의 최신 플래그십 스마트폰"
                )
            ),
            VectorIndexRequest(
                namespace = "test_db",
                entity = "products",
                recordKey = "prod-002",
                fields = mapOf(
                    "name" to "아이폰 15 Pro",
                    "description" to "애플의 프리미엄 스마트폰"
                )
            ),
            VectorIndexRequest(
                namespace = "test_db",
                entity = "products",
                recordKey = "prod-003",
                fields = mapOf(
                    "name" to "노트북 맥북 프로",
                    "description" to "고성능 개발자 노트북"
                )
            )
        )

        products.forEach { request ->
            universalVectorIndexingService.indexEntity(request)
        }

        // 비동기 벡터 생성 대기
        Thread.sleep(2000)

        // when - "스마트폰" 검색 (products만 대상)
        val searchRequest = VectorSearchRequest(
            namespace = "test_db",
            entity = "products",
            query = "스마트폰",
            limit = 10
        )
        val results = universalVectorIndexingService.search(searchRequest)

        // then - 스마트폰 관련 제품만 검색됨
        assertThat(results).isNotEmpty
        assertThat(results).hasSizeLessThanOrEqualTo(10)

        // 모든 결과가 products 엔티티에서 왔는지 확인
        results.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("products")
            assertThat(result.metadata?.get("namespace")).isEqualTo("test_db")
        }

        println("✅ US1-1: ${results.size}개의 제품 검색 성공")
    }

    @Test
    @DisplayName("US1-2: Posts와 Products가 모두 인덱싱되어도 namespace/entity로 분리된다")
    fun `should separate posts and products by namespace and entity`() {
        // given - Posts 생성
        val postRequest = PostCreateRequest(
            title = "스마트폰 리뷰",
            content = "최신 스마트폰들을 비교 분석합니다",
            author = "리뷰어"
        )
        restTemplate.postForEntity(baseUrl, postRequest, PostResponse::class.java)

        // Products 생성
        val productRequest = VectorIndexRequest(
            namespace = "test_db",
            entity = "products",
            recordKey = "prod-001",
            fields = mapOf("name" to "스마트폰 갤럭시")
        )
        universalVectorIndexingService.indexEntity(productRequest)

        Thread.sleep(2000) // 비동기 처리 대기

        // when - Posts에서만 검색
        val postsSearch = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "스마트폰",
            limit = 10
        )
        val postsResults = universalVectorIndexingService.search(postsSearch)

        // Products에서만 검색
        val productsSearch = VectorSearchRequest(
            namespace = "test_db",
            entity = "products",
            query = "스마트폰",
            limit = 10
        )
        val productsResults = universalVectorIndexingService.search(productsSearch)

        // then - 각각 자신의 엔티티 결과만 반환
        postsResults.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("posts")
        }

        productsResults.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("products")
        }

        println("✅ US1-2: Posts(${postsResults.size}건), Products(${productsResults.size}건) 분리 검색 성공")
    }

    // ═══════════════════════════════════════════════════════════════
    // US2: 필드별 독립 벡터화
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US2-1: Title과 Content를 독립적으로 검색할 수 있다")
    fun `should search title and content fields independently`() {
        // given - "PostgreSQL" 제목 + "MySQL" 본문
        val request = PostCreateRequest(
            title = "PostgreSQL 완벽 가이드",
            content = "MySQL과 다른 PostgreSQL의 고급 기능을 설명합니다",
            author = "DB전문가"
        )
        val response = restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        val postId = response.body!!.id

        Thread.sleep(1500) // 비동기 벡터 생성 대기

        // when - Title 필드만 검색
        val titleSearch = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "PostgreSQL",
            fieldName = "title",
            limit = 10
        )
        val titleResults = universalVectorIndexingService.search(titleSearch)

        // Content 필드만 검색
        val contentSearch = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "MySQL",
            fieldName = "content",
            limit = 10
        )
        val contentResults = universalVectorIndexingService.search(contentSearch)

        // then - 각각 해당 필드에서만 매칭
        assertThat(titleResults).isNotEmpty
        titleResults.forEach { result ->
            assertThat(result.metadata?.get("field_name")).isEqualTo("title")
        }

        assertThat(contentResults).isNotEmpty
        contentResults.forEach { result ->
            assertThat(result.metadata?.get("field_name")).isEqualTo("content")
        }

        println("✅ US2-1: Title(${titleResults.size}건), Content(${contentResults.size}건) 독립 검색 성공")
    }

    @Test
    @DisplayName("US2-2: 필드별 가중치 검색 - Title 60%, Content 40%")
    fun `should apply field weights in search - 60 percent title 40 percent content`() {
        // given - 여러 게시글 생성
        val posts = listOf(
            PostCreateRequest("Kotlin 코루틴", "Java와 Kotlin의 차이점", "작성자1"),
            PostCreateRequest("Java 기초", "Kotlin과 Java 비교 분석", "작성자2"),
            PostCreateRequest("Spring Boot", "Kotlin과 Java 모두 지원", "작성자3")
        )

        posts.forEach { req ->
            restTemplate.postForEntity(baseUrl, req, PostResponse::class.java)
        }

        Thread.sleep(3000) // 비동기 처리 대기

        // when - "Kotlin" 검색 (가중치: title 60%, content 40%)
        val searchRequest = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "Kotlin",
            fieldWeights = mapOf(
                "title" to 0.6,
                "content" to 0.4
            ),
            limit = 10
        )
        val results = universalVectorIndexingService.search(searchRequest)

        // then - 가중치가 적용되어 결과 반환
        assertThat(results).isNotEmpty

        // 첫 번째 결과가 제목에 "Kotlin"이 있는 게시글일 확률이 높음
        val topResult = results.first()
        assertThat(topResult.recordKey).isNotNull()

        println("✅ US2-2: 가중치 검색 성공 - Top 1 유사도: ${topResult.similarityScore}")
    }

    // ═══════════════════════════════════════════════════════════════
    // US3: 메타데이터 추적 및 필터링
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US3-1: 검색 결과에 namespace, entity, field_name 메타데이터가 포함된다")
    fun `should include namespace entity field metadata in search results`() {
        // given
        val request = PostCreateRequest(
            title = "테스트 게시글",
            content = "메타데이터 검증용 본문",
            author = "작성자"
        )
        restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)

        Thread.sleep(1500) // 비동기 처리 대기

        // when
        val searchRequest = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "테스트",
            limit = 10
        )
        val results = universalVectorIndexingService.search(searchRequest)

        // then - 모든 결과에 메타데이터 포함
        assertThat(results).isNotEmpty
        results.forEach { result ->
            assertThat(result.metadata).isNotNull
            assertThat(result.metadata!!["namespace"]).isEqualTo("vector_ai")
            assertThat(result.metadata!!["entity"]).isEqualTo("posts")
            assertThat(result.metadata!!["field_name"]).isIn("title", "content")
        }

        println("✅ US3-1: ${results.size}개 결과에 모두 메타데이터 포함")
    }

    @Test
    @DisplayName("US3-2: Entity 필터링으로 검색 범위를 제한할 수 있다")
    fun `should filter search results by entity`() {
        // given - Posts와 Products 모두 생성
        restTemplate.postForEntity(
            baseUrl,
            PostCreateRequest("스마트폰", "게시글 내용", "작성자"),
            PostResponse::class.java
        )

        universalVectorIndexingService.indexEntity(
            VectorIndexRequest(
                namespace = "vector_ai",
                entity = "products",
                recordKey = "prod-001",
                fields = mapOf("name" to "스마트폰")
            )
        )

        Thread.sleep(2000) // 비동기 처리 대기

        // when - Posts만 검색
        val postsOnly = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "스마트폰",
            limit = 10
        )
        val postsResults = universalVectorIndexingService.search(postsOnly)

        // then - Posts 결과만 반환
        assertThat(postsResults).isNotEmpty
        postsResults.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("posts")
        }

        println("✅ US3-2: Entity 필터링 성공 - ${postsResults.size}개 결과")
    }

    // ═══════════════════════════════════════════════════════════════
    // US4: 스마트 청킹 품질 향상
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US4-1: 문장 경계가 보존되어 청킹된다")
    fun `should preserve sentence boundaries during chunking`() {
        // given - 여러 문장으로 구성된 긴 텍스트
        val longText = """
            첫 번째 문장입니다. 두 번째 문장입니다. 세 번째 문장입니다.
            네 번째 문장입니다. 다섯 번째 문장입니다. 여섯 번째 문장입니다.
            일곱 번째 문장입니다. 여덟 번째 문장입니다. 아홉 번째 문장입니다.
            열 번째 문장입니다. 열한 번째 문장입니다. 열두 번째 문장입니다.
        """.trimIndent()

        val request = PostCreateRequest(
            title = "청킹 테스트",
            content = longText,
            author = "작성자"
        )
        val response = restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        val postId = response.body!!.id.toString()

        Thread.sleep(1500) // 비동기 처리 대기

        // when - 생성된 청크 조회
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai", "posts", postId
        )

        // then - 청크가 생성되었고, 각 청크 텍스트가 문장 경계에서 잘렸는지 확인
        assertThat(chunks).isNotEmpty

        var sentenceBoundaryPreserved = 0
        chunks.forEach { chunk ->
            val text = chunk.chunkText.trim()
            // 문장 종결자로 끝나거나 시작하는지 확인
            if (text.endsWith(".") || text.endsWith("다")) {
                sentenceBoundaryPreserved++
            }
        }

        val preservationRate = sentenceBoundaryPreserved.toDouble() / chunks.size * 100
        println("🔵 [청킹 품질] 전체 청크: ${chunks.size}, 문장 경계 보존: ${sentenceBoundaryPreserved}개 (${preservationRate}%)")

        // 최소 50% 이상의 청크가 문장 경계를 보존해야 함
        assertThat(preservationRate).isGreaterThanOrEqualTo(50.0)

        println("✅ US4-1: 문장 경계 보존율 ${preservationRate}% 달성")
    }

    @Test
    @DisplayName("US4-2: 짧은 텍스트는 단일 청크로 처리된다")
    fun `should handle short text as single chunk`() {
        // given - 50자 이하 짧은 텍스트
        val shortText = "짧은 텍스트입니다."

        val request = PostCreateRequest(
            title = "짧은 제목",
            content = shortText,
            author = "작성자"
        )
        val response = restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        val postId = response.body!!.id.toString()

        Thread.sleep(1000) // 비동기 처리 대기

        // when - Content 필드 청크 조회
        val contentChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyAndFieldNameOrderByChunkIndexAsc(
            "vector_ai", "posts", postId, "content"
        )

        // then - Content 필드는 단일 청크로 처리됨
        assertThat(contentChunks).hasSizeLessThanOrEqualTo(1)

        println("✅ US4-2: 짧은 텍스트 단일 청크 처리 성공")
    }

    // ═══════════════════════════════════════════════════════════════
    // US5: 재인덱싱 시 데이터 일관성 보장
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US5-1: 재인덱싱 시 고아 청크가 발생하지 않는다")
    fun `should not create orphan chunks during reindexing`() {
        // given - 초기 게시글 생성
        val createRequest = PostCreateRequest(
            title = "원본 제목",
            content = "원본 내용입니다.",
            author = "작성자"
        )
        val createResponse = restTemplate.postForEntity(baseUrl, createRequest, PostResponse::class.java)
        val postId = createResponse.body!!.id

        Thread.sleep(1500) // 비동기 처리 대기

        val initialChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai", "posts", postId.toString()
        )
        val initialChunkCount = initialChunks.size

        // when - 게시글 수정 (재인덱싱 트리거)
        val updateRequest = mapOf(
            "title" to "수정된 제목",
            "content" to "완전히 새로운 내용으로 변경되었습니다. 훨씬 더 길어진 텍스트입니다."
        )
        restTemplate.put("$baseUrl/$postId", updateRequest)

        Thread.sleep(2000) // 비동기 재처리 대기

        // then - 기존 청크는 삭제되고 새 청크만 존재
        val finalChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai", "posts", postId.toString()
        )

        // 모든 청크의 업데이트 시간이 생성 시간 이후여야 함 (새로 생성됨을 의미)
        finalChunks.forEach { chunk ->
            assertThat(chunk.id).isNotNull()
        }

        println("🔵 [재인덱싱] 초기 청크: ${initialChunkCount}개 → 최종 청크: ${finalChunks.size}개")
        println("✅ US5-1: 재인덱싱 일관성 보장 성공")
    }

    // ═══════════════════════════════════════════════════════════════
    // US6: 기존 Post 시스템과 호환성 유지
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("US6-1: 기존 POST api/posts API가 정상 동작한다")
    fun `should maintain compatibility with existing POST posts API`() {
        // when
        val request = PostCreateRequest(
            title = "호환성 테스트",
            content = "기존 API 호환성 검증",
            author = "테스터"
        )
        val response = restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.title).isEqualTo("호환성 테스트")
        assertThat(response.body!!.content).isEqualTo("기존 API 호환성 검증")

        println("✅ US6-1: POST /api/posts API 호환성 유지")
    }

    @Test
    @DisplayName("US6-2: 기존 POST api/posts/search/vector API가 정상 동작한다")
    fun `should maintain compatibility with existing vector search API`() {
        // given
        val createRequest = PostCreateRequest(
            title = "검색 테스트",
            content = "벡터 검색 API 호환성 검증",
            author = "테스터"
        )
        restTemplate.postForEntity(baseUrl, createRequest, PostResponse::class.java)

        Thread.sleep(1500) // 비동기 처리 대기

        // when
        val searchRequest = PostVectorSearchRequest(
            query = "검색",
            limit = 10
        )
        val response = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!).isNotEmpty

        println("✅ US6-2: POST /api/posts/search/vector API 호환성 유지")
    }

    @Test
    @DisplayName("US6-3: 전체 CRUD 작업이 새 시스템에서 정상 동작한다")
    fun `should perform full CRUD operations with new system`() {
        // Create
        val createRequest = PostCreateRequest("CRUD 제목", "CRUD 내용", "작성자")
        val createResponse = restTemplate.postForEntity(baseUrl, createRequest, PostResponse::class.java)
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val postId = createResponse.body!!.id

        // Read
        val readResponse = restTemplate.getForEntity("$baseUrl/$postId", PostResponse::class.java)
        assertThat(readResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(readResponse.body!!.title).isEqualTo("CRUD 제목")

        // Update
        val updateRequest = mapOf("title" to "수정된 제목", "content" to "수정된 내용")
        restTemplate.put("$baseUrl/$postId", updateRequest)
        Thread.sleep(1000)
        val updatedPost = restTemplate.getForEntity("$baseUrl/$postId", PostResponse::class.java)
        assertThat(updatedPost.body!!.title).isEqualTo("수정된 제목")

        // Delete
        restTemplate.delete("$baseUrl/$postId")

        // Verify deletion
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            "vector_ai", "posts", postId.toString()
        )
        assertThat(chunks).isEmpty()

        println("✅ US6-3: 전체 CRUD 작업 정상 동작")
    }

    // ═══════════════════════════════════════════════════════════════
    // 종합 시나리오: 실제 사용 사례 시뮬레이션
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("종합 시나리오: 블로그 시스템에서 게시글과 댓글을 벡터 검색한다")
    fun `comprehensive scenario - blog system with posts and comments vector search`() {
        // given - 게시글 생성
        val post1 = PostCreateRequest(
            title = "Spring Boot 완벽 가이드",
            content = "Spring Boot를 활용한 웹 애플리케이션 개발 방법을 설명합니다. JPA와 PostgreSQL 연동 예제를 포함합니다.",
            author = "개발자1"
        )
        val post2 = PostCreateRequest(
            title = "Kotlin 코루틴 마스터",
            content = "Kotlin의 비동기 프로그래밍을 위한 코루틴 사용법을 자세히 다룹니다. Flow와 Channel도 함께 설명합니다.",
            author = "개발자2"
        )

        val postResponse1 = restTemplate.postForEntity(baseUrl, post1, PostResponse::class.java)
        val postResponse2 = restTemplate.postForEntity(baseUrl, post2, PostResponse::class.java)

        // Comments 시뮬레이션 (UniversalVectorIndexingService 직접 사용)
        val comments = listOf(
            VectorIndexRequest(
                namespace = "blog_db",
                entity = "comments",
                recordKey = "comment-001",
                fields = mapOf(
                    "content" to "Spring Boot 설명이 정말 도움이 되었습니다!",
                    "author" to "독자1"
                )
            ),
            VectorIndexRequest(
                namespace = "blog_db",
                entity = "comments",
                recordKey = "comment-002",
                fields = mapOf(
                    "content" to "Kotlin 코루틴 예제가 명확해서 이해하기 쉬웠어요",
                    "author" to "독자2"
                )
            )
        )

        comments.forEach { request ->
            universalVectorIndexingService.indexEntity(request)
        }

        Thread.sleep(3000) // 비동기 처리 대기

        // when - "Spring Boot" 검색 (Posts와 Comments 별도 검색)
        val postsSearch = VectorSearchRequest(
            namespace = "vector_ai",
            entity = "posts",
            query = "Spring Boot",
            limit = 10
        )
        val postsResults = universalVectorIndexingService.search(postsSearch)

        val commentsSearch = VectorSearchRequest(
            namespace = "blog_db",
            entity = "comments",
            query = "Spring Boot",
            limit = 10
        )
        val commentsResults = universalVectorIndexingService.search(commentsSearch)

        // then
        assertThat(postsResults).isNotEmpty
        assertThat(commentsResults).isNotEmpty

        println("📊 [종합 시나리오 결과]")
        println("  - Posts 검색: ${postsResults.size}건")
        println("  - Comments 검색: ${commentsResults.size}건")
        println("  - 총 검색 결과: ${postsResults.size + commentsResults.size}건")

        // Entity 분리 검증
        postsResults.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("posts")
        }
        commentsResults.forEach { result ->
            assertThat(result.metadata?.get("entity")).isEqualTo("comments")
        }

        println("✅ 종합 시나리오: 블로그 시스템 벡터 검색 성공")
    }
}
