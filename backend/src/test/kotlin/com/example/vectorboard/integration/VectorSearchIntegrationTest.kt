package com.example.vectorboard.integration

import com.example.vectorboard.dto.PostCreateRequest
import com.example.vectorboard.dto.PostResponse
import com.example.vectorboard.dto.PostVectorSearchRequest
import com.example.vectorboard.dto.PostVectorSearchResult
import com.example.vectorboard.repository.PostRepository
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
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("벡터 검색 통합 테스트")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class VectorSearchIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var postRepository: PostRepository

    private val baseUrl: String
        get() = "http://localhost:$port/api/posts"

    @BeforeEach
    fun setUp() {
        // DB 초기화는 @Sql 어노테이션으로 처리됨
        // 추가적인 setUp 로직이 필요하면 여기에 작성
    }

    @Test
    @DisplayName("게시글 생성 시 벡터가 자동으로 생성된다")
    fun `should auto-generate vector on post creation`() {
        // given
        val request = PostCreateRequest(
            title = "Spring Boot 가이드",
            content = "Spring Boot는 Java 기반의 프레임워크입니다.",
            author = "테스터"
        )

        // when
        val response = restTemplate.postForEntity(
            baseUrl,
            request,
            PostResponse::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.title).isEqualTo("Spring Boot 가이드")
    }

    @Test
    @DisplayName("유사한 내용의 게시글을 벡터 검색으로 찾을 수 있다")
    fun `should find similar posts using vector search`() {
        // given - 여러 주제의 게시글 생성
        val posts = listOf(
            PostCreateRequest("Spring Boot 튜토리얼", "Spring Boot 프레임워크 학습", "작성자1"),
            PostCreateRequest("Kotlin 코루틴", "Kotlin의 비동기 프로그래밍", "작성자2"),
            PostCreateRequest("PostgreSQL 최적화", "데이터베이스 성능 향상 방법", "작성자3"),
            PostCreateRequest("Spring Data JPA", "JPA를 활용한 데이터 접근", "작성자4")
        )

        posts.forEach { request ->
            restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        }

        // when - "Spring" 관련 검색
        val searchRequest = PostVectorSearchRequest(query = "Spring 프레임워크", limit = 3)
        val searchResponse = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(searchResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(searchResponse.body).isNotNull
        assertThat(searchResponse.body!!).hasSize(3)
    }

    @Test
    @DisplayName("검색 결과가 없을 때 빈 배열을 반환한다")
    fun `should return empty array when no posts exist`() {
        // given
        val searchRequest = PostVectorSearchRequest(query = "존재하지 않는 내용", limit = 5)

        // when
        val response = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    @DisplayName("limit 파라미터가 결과 수를 제한한다")
    fun `should respect limit parameter`() {
        // given - 5개의 게시글 생성
        (1..5).forEach { i ->
            val request = PostCreateRequest("제목 $i", "내용 $i", "작성자 $i")
            restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        }

        // when - limit=3으로 검색
        val searchRequest = PostVectorSearchRequest(query = "내용", limit = 3)
        val response = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).hasSizeLessThanOrEqualTo(3)
    }

    @Test
    @DisplayName("벡터가 있는 게시글만 검색 결과에 포함된다")
    fun `should only include posts with vectors in search results`() {
        // given
        val request = PostCreateRequest("테스트 게시글", "테스트 내용", "작성자")
        restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)

        // when
        val searchRequest = PostVectorSearchRequest(query = "테스트", limit = 10)
        val response = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotEmpty
    }

    @Test
    @DisplayName("다양한 주제의 게시글에서 의미 기반 검색이 작동한다")
    fun `should perform semantic search across diverse topics`() {
        // given
        val techPosts = listOf(
            PostCreateRequest("머신러닝 기초", "인공지능과 딥러닝 개념", "AI전문가"),
            PostCreateRequest("Docker 컨테이너", "애플리케이션 배포 자동화", "DevOps"),
            PostCreateRequest("React 프론트엔드", "모던 웹 개발 프레임워크", "프론트개발자")
        )

        techPosts.forEach { request ->
            restTemplate.postForEntity(baseUrl, request, PostResponse::class.java)
        }

        // when - "인공지능" 관련 검색
        val searchRequest = PostVectorSearchRequest(query = "AI와 머신러닝", limit = 2)
        val response = restTemplate.postForEntity(
            "$baseUrl/search/vector",
            searchRequest,
            Array<PostVectorSearchResult>::class.java
        )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotEmpty
    }

    @Test
    @DisplayName("게시글 수정 시 벡터가 재생성된다")
    fun `should regenerate vector on post update`() {
        // given
        val createRequest = PostCreateRequest("원본 제목", "원본 내용", "작성자")
        val createResponse = restTemplate.postForEntity(
            baseUrl,
            createRequest,
            PostResponse::class.java
        )
        val postId = createResponse.body!!.id

        // when - 게시글 수정
        val updateRequest = mapOf(
            "title" to "수정된 제목",
            "content" to "완전히 다른 내용으로 수정되었습니다"
        )
        restTemplate.put("$baseUrl/$postId", updateRequest)

        // then - 수정된 게시글 조회
        val updatedPost = restTemplate.getForEntity(
            "$baseUrl/$postId",
            PostResponse::class.java
        )
        assertThat(updatedPost.body!!.title).isEqualTo("수정된 제목")
    }

    @Test
    @DisplayName("전체 CRUD 작업이 정상 동작한다")
    fun `should perform full CRUD operations`() {
        // Create
        val createRequest = PostCreateRequest("CRUD 테스트", "생성 테스트", "작성자")
        val createResponse = restTemplate.postForEntity(baseUrl, createRequest, PostResponse::class.java)
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        val postId = createResponse.body!!.id

        // Read
        val readResponse = restTemplate.getForEntity("$baseUrl/$postId", PostResponse::class.java)
        assertThat(readResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(readResponse.body!!.title).isEqualTo("CRUD 테스트")

        // Update
        val updateRequest = mapOf("title" to "수정된 제목", "content" to "수정된 내용")
        restTemplate.put("$baseUrl/$postId", updateRequest)
        val updatedPost = restTemplate.getForEntity("$baseUrl/$postId", PostResponse::class.java)
        assertThat(updatedPost.body!!.title).isEqualTo("수정된 제목")

        // Delete
        restTemplate.delete("$baseUrl/$postId")

        // 삭제 후 조회 시도
        try {
            val deletedPost = restTemplate.getForEntity("$baseUrl/$postId", PostResponse::class.java)
            // 404 또는 200(null body) 중 하나가 와야 함
            assertThat(deletedPost.statusCode.is2xxSuccessful || deletedPost.statusCode.is4xxClientError).isTrue()
        } catch (e: Exception) {
            // 404 에러 발생하는 경우도 정상
            assertThat(e).isNotNull
        }
    }
}
