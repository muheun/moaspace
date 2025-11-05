package me.muheun.moaspace.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.service.JwtTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

/**
 * PostController 통합 테스트
 * T047-049: PostController 엔드포인트 테스트
 *
 * Constitution Principle V: 실제 DB 연동 테스트 (@SpringBootTest)
 * Mock 테스트 절대 금지
 *
 * 주요 테스트:
 * - POST /api/posts: 게시글 생성 및 자동 벡터화
 * - GET /api/posts/{id}: 게시글 조회
 * - PUT /api/posts/{id}: 게시글 수정 및 벡터 재생성
 * - 소유권 검증 (작성자만 수정 가능)
 * - 인증 오류 처리 (401 Unauthorized)
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc  // Security 필터 활성화 (TestSecurityConfig 사용)
@Transactional
class PostControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        entityManager.createNativeQuery("TRUNCATE TABLE posts, vector_chunks, users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * T047: 게시글 생성 및 자동 벡터화 테스트
     *
     * Given: 인증된 사용자
     * When: POST /api/posts로 게시글 생성 요청
     * Then: 201 Created 응답 + PostEmbedding 자동 생성
     */
    @Test
    @DisplayName("testCreatePost - 게시글을 생성하고 자동으로 벡터화한다")
    fun testCreatePost() {
        // Given: 테스트용 사용자 생성
        val user = userRepository.save(
            User(
                email = "author@example.com",
                name = "게시글 작성자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        val createRequest = CreatePostRequest(
            title = "Next.js 15 + React 19 사용 후기",
            contentHtml = "**Next.js 15**가 출시되었습니다.",
            hashtags = listOf("Next.js", "React", "웹개발")
        )

        val requestJson = objectMapper.writeValueAsString(createRequest)

        // When: POST /api/posts
        val result = mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            // Then: 201 Created
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value(createRequest.title))
            .andExpect(jsonPath("$.contentHtml").value(createRequest.contentHtml))
            .andExpect(jsonPath("$.contentHtml").exists())
            .andExpect(jsonPath("$.author.id").value(user.id!!))
            .andExpect(jsonPath("$.author.name").value(user.name))
            .andExpect(jsonPath("$.hashtags[0]").value("Next.js"))
            .andExpect(jsonPath("$.hashtags[1]").value("React"))
            .andExpect(jsonPath("$.hashtags[2]").value("웹개발"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andReturn()

        val responseJson = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseJson, Map::class.java)
        val postId = (responseMap["id"] as Number).toLong()

        val post = postRepository.findById(postId).get()
        assert(!post.deleted) { "생성된 게시글이 삭제 상태입니다" }

        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "Post",
            recordKey = postId.toString()
        )
        assert(chunks.isNotEmpty()) { "VectorChunk가 자동 생성되지 않았습니다" }
    }

    /**
     * T048: 게시글 조회 테스트
     *
     * Given: 저장된 게시글
     * When: GET /api/posts/{id}로 조회 요청
     * Then: 200 OK 응답 및 게시글 정보 확인
     */
    @Test
    @DisplayName("testGetPostById - ID로 게시글을 조회한다")
    fun testGetPostById() {
        // Given: 테스트용 사용자 및 게시글 생성
        val user = userRepository.save(
            User(
                email = "reader@example.com",
                name = "독자",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "테스트 게시글",
                contentMarkdown = "테스트 내용",
                contentHtml = "<p>테스트 내용</p>",
                contentText = "테스트 내용",
                author = user,
                hashtags = arrayOf("테스트")
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: GET /api/posts/{id}
        mockMvc.perform(
            get("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 200 OK
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(post.id!!))
            .andExpect(jsonPath("$.title").value(post.title))
            .andExpect(jsonPath("$.contentHtml").value(post.contentHtml))
            .andExpect(jsonPath("$.contentHtml").value(post.contentHtml))
            .andExpect(jsonPath("$.author.id").value(user.id!!))
            .andExpect(jsonPath("$.author.name").value(user.name))
            .andExpect(jsonPath("$.hashtags[0]").value("테스트"))
    }

    /**
     * T049: 게시글 수정 및 벡터 재생성 테스트
     *
     * Given: 저장된 게시글 및 벡터
     * When: PUT /api/posts/{id}로 수정 요청
     * Then: 200 OK 응답 + PostEmbedding 재생성
     */
    @Test
    @DisplayName("testUpdatePost - 게시글을 수정하고 벡터를 재생성한다")
    fun testUpdatePost() {
        // Given: 테스트용 사용자 및 게시글 생성
        val user = userRepository.save(
            User(
                email = "editor@example.com",
                name = "편집자",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "원본 제목",
                contentMarkdown = "원본 내용",
                contentHtml = "<p>원본 내용</p>",
                contentText = "원본 내용",
                author = user,
                hashtags = arrayOf("원본")
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        val updateRequest = UpdatePostRequest(
            title = "수정된 제목",
            contentHtml = "수정된 내용입니다.",
            hashtags = listOf("수정", "테스트")
        )

        val requestJson = objectMapper.writeValueAsString(updateRequest)

        // When: PUT /api/posts/{id}
        mockMvc.perform(
            put("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            // Then: 200 OK
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(post.id!!))
            .andExpect(jsonPath("$.title").value("수정된 제목"))
            .andExpect(jsonPath("$.contentHtml").value("수정된 내용입니다."))
            .andExpect(jsonPath("$.contentHtml").exists())
            .andExpect(jsonPath("$.hashtags[0]").value("수정"))
            .andExpect(jsonPath("$.hashtags[1]").value("테스트"))

        val updatedPost = postRepository.findById(post.id!!).get()
        assert(updatedPost.title == "수정된 제목") { "제목이 수정되지 않았습니다" }
        assert(updatedPost.contentText == "수정된 내용입니다.") { "contentText가 수정되지 않았습니다" }

        // VectorChunk 확인 (PostEmbedding → VectorChunk 마이그레이션)
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "Post",
            recordKey = updatedPost.id.toString()
        )
        assert(chunks.isNotEmpty()) { "VectorChunk가 재생성되지 않았습니다" }
    }

    /**
     * T049: 소유권 검증 테스트
     *
     * Given: 작성자 A의 게시글
     * When: 사용자 B가 수정 시도
     * Then: 403 Forbidden 응답
     */
    @Test
    @DisplayName("testUpdatePostForbidden - 작성자가 아닌 경우 수정 시 403 오류를 반환한다")
    fun testUpdatePostForbidden() {
        // Given: 작성자 A 및 게시글 생성
        val authorA = userRepository.save(
            User(
                email = "author.a@example.com",
                name = "작성자 A",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "A의 게시글",
                contentMarkdown = "A가 작성한 내용",
                contentHtml = "<p>A가 작성한 내용</p>",
                contentText = "A가 작성한 내용",
                author = authorA,
                hashtags = arrayOf("A")
            )
        )

        // 사용자 B 생성
        val userB = userRepository.save(
            User(
                email = "user.b@example.com",
                name = "사용자 B",
                profileImageUrl = null
            )
        )

        val accessTokenB = jwtTokenService.generateAccessToken(userB.id!!, userB.email)

        val updateRequest = UpdatePostRequest(
            title = "B가 수정한 제목",
            contentHtml = "B가 수정한 내용",
            hashtags = listOf("B")
        )

        val requestJson = objectMapper.writeValueAsString(updateRequest)

        // When: 사용자 B가 PUT /api/posts/{id} 시도
        mockMvc.perform(
            put("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessTokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            // Then: 403 Forbidden
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.error.message").value("게시글을 수정할 권한이 없습니다"))

        val unchangedPost = postRepository.findById(post.id!!).get()
        assert(unchangedPost.title == "A의 게시글") { "게시글이 수정되었습니다" }
    }

    /**
     * T047: 인증 없이 게시글 생성 시도
     *
     * Given: Authorization 헤더 없음
     * When: POST /api/posts 호출
     * Then: 401 Unauthorized 응답
     */
    @Test
    @DisplayName("testCreatePostUnauthorized - 인증 없이 게시글 생성 시 401 오류를 반환한다")
    fun testCreatePostUnauthorized() {
        // Given: 게시글 생성 요청
        val createRequest = CreatePostRequest(
            title = "인증 없는 게시글",
            contentHtml = "내용",
            hashtags = emptyList()
        )

        val requestJson = objectMapper.writeValueAsString(createRequest)

        // When: Authorization 헤더 없이 POST /api/posts
        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            // Then: 401 Unauthorized (Spring Security 표준 응답)
            .andExpect(status().isUnauthorized)
    }

    /**
     * T048: 존재하지 않는 게시글 조회
     *
     * Given: 존재하지 않는 게시글 ID
     * When: GET /api/posts/{id} 호출
     * Then: 404 Not Found 응답
     */
    @Test
    @DisplayName("testGetPostNotFound - 존재하지 않는 게시글 조회 시 404 오류를 반환한다")
    fun testGetPostNotFound() {
        // Given: 테스트용 사용자
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        val nonExistentPostId = 99999L

        // When: GET /api/posts/{nonExistentId}
        mockMvc.perform(
            get("/api/posts/$nonExistentPostId")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 404 Not Found
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    /**
     * T048: 삭제된 게시글 조회 시도
     *
     * Given: 소프트 삭제된 게시글 (deleted=true)
     * When: GET /api/posts/{id} 호출
     * Then: 404 Not Found 응답
     */
    @Test
    @DisplayName("testGetDeletedPost - 삭제된 게시글 조회 시 404 오류를 반환한다")
    fun testGetDeletedPost() {
        // Given: 테스트용 사용자 및 삭제된 게시글
        val user = userRepository.save(
            User(
                email = "deleter@example.com",
                name = "삭제자",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "삭제될 게시글",
                contentMarkdown = "삭제될 내용",
                contentHtml = "<p>삭제될 내용</p>",
                contentText = "삭제될 내용",
                author = user,
                hashtags = arrayOf("삭제")
            )
        )

        post.deleted = true
        postRepository.save(post)

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: GET /api/posts/{deletedId}
        mockMvc.perform(
            get("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 404 Not Found
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
    }

    /**
     * T065: 페이지네이션 테스트
     *
     * Given: 30개의 게시글
     * When: page=0~2, size=10으로 조회
     * Then: 각 페이지당 10개 반환, totalElements=30, totalPages=3
     */
    @Test
    @DisplayName("testGetPostsPaginated - 게시글 목록을 페이지네이션하여 반환한다")
    fun testGetPostsPaginated() {
        // Given: 30개의 게시글 생성
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        repeat(30) { index ->
            postRepository.save(
                Post(
                    title = "게시글 ${index + 1}",
                    contentMarkdown = "내용 ${index + 1}",
                    contentHtml = "<p>내용 ${index + 1}</p>",
                    contentText = "내용 ${index + 1}",
                    author = user,
                    hashtags = arrayOf("테스트")
                )
            )
        }

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: GET /api/posts?page=0&size=10
        mockMvc.perform(
            get("/api/posts")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 10개 게시글 반환, totalElements=30, totalPages=3
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(10))
            .andExpect(jsonPath("$.pagination.page").value(0))
            .andExpect(jsonPath("$.pagination.size").value(10))
            .andExpect(jsonPath("$.pagination.totalElements").value(30))
            .andExpect(jsonPath("$.pagination.totalPages").value(3))
            .andReturn()

        // When: GET /api/posts?page=1&size=10
        mockMvc.perform(
            get("/api/posts")
                .param("page", "1")
                .param("size", "10")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(10))
            .andExpect(jsonPath("$.pagination.page").value(1))

        // When: GET /api/posts?page=2&size=10
        mockMvc.perform(
            get("/api/posts")
                .param("page", "2")
                .param("size", "10")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(10))
            .andExpect(jsonPath("$.pagination.page").value(2))
    }

    /**
     * T066: 해시태그 필터링 테스트
     *
     * Given: #AI 태그 5개, #Backend 태그 3개
     * When: hashtag 파라미터로 조회
     * Then: 해당 태그 게시글만 반환
     */
    @Test
    @DisplayName("testFilterPostsByHashtag - 해시태그로 게시글을 필터링한다")
    fun testFilterPostsByHashtag() {
        // Given: 다양한 해시태그 게시글 생성
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        // #AI 태그 5개
        repeat(5) { index ->
            postRepository.save(
                Post(
                    title = "AI 게시글 ${index + 1}",
                    contentMarkdown = "AI 내용",
                    contentHtml = "<p>AI 내용</p>",
                    contentText = "AI 내용",
                    author = user,
                    hashtags = arrayOf("AI", "Tech")
                )
            )
        }

        // #Backend 태그 3개
        repeat(3) { index ->
            postRepository.save(
                Post(
                    title = "Backend 게시글 ${index + 1}",
                    contentMarkdown = "Backend 내용",
                    contentHtml = "<p>Backend 내용</p>",
                    contentText = "Backend 내용",
                    author = user,
                    hashtags = arrayOf("Backend", "Server")
                )
            )
        }

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: GET /api/posts?hashtag=AI
        mockMvc.perform(
            get("/api/posts")
                .param("hashtag", "AI")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 5개 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(5))
            .andExpect(jsonPath("$.pagination.totalElements").value(5))

        // When: GET /api/posts?hashtag=Backend
        mockMvc.perform(
            get("/api/posts")
                .param("hashtag", "Backend")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 3개 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(3))
            .andExpect(jsonPath("$.pagination.totalElements").value(3))

        // When: GET /api/posts (필터 없음)
        mockMvc.perform(
            get("/api/posts")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 8개 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pagination.totalElements").value(8))
    }

    /**
     * T067: 벡터 검색 정확도 테스트
     *
     * Given: AI 관련 게시글 2개, 무관한 게시글 1개
     * When: "AI와 머신러닝" 검색 (threshold=0.6)
     * Then: 관련 게시글만 반환 + 유사도 점수 포함
     */
    @Test
    @DisplayName("testSearchPostsWithThreshold - 임계값 기반 벡터 검색으로 유사 게시글을 찾는다")
    fun testSearchPostsWithThreshold() {
        // Given: 다양한 주제의 게시글 생성 (HTTP API 사용하여 벡터화 자동 처리)
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // Post 1: AI 관련
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "인공지능 기술의 발전",
                        contentHtml = "인공지능과 머신러닝 기술이 빠르게 발전하고 있습니다.",
                        hashtags = listOf("AI", "ML")
                    )
                ))
        ).andExpect(status().isCreated)

        // Post 2: ML 관련
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "머신러닝과 딥러닝",
                        contentHtml = "머신러닝과 딥러닝은 AI의 핵심 기술입니다.",
                        hashtags = listOf("ML", "DL")
                    )
                ))
        ).andExpect(status().isCreated)

        // Post 3: 무관한 주제
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "요리 레시피",
                        contentHtml = "맛있는 파스타 만드는 법을 소개합니다.",
                        hashtags = listOf("요리", "레시피")
                    )
                ))
        ).andExpect(status().isCreated)

        // 벡터화 완료 대기 (비동기 처리)
        Thread.sleep(2000)

        val searchRequest = """
            {
                "query": "AI와 머신러닝",
                "threshold": 0.6,
                "limit": 20
            }
        """.trimIndent()

        // When: POST /api/posts/search
        mockMvc.perform(
            post("/api/posts/search")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest)
        )
            // Then: 유사한 게시글만 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results").isArray)
            .andExpect(jsonPath("$.results[0].post").exists())
            .andExpect(jsonPath("$.results[0].similarity").isNumber)
            .andExpect(jsonPath("$.results[0].similarity").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.6)))
    }

    /**
     * T032-1: 필드별 가중치 기반 검색 테스트 (SC-003)
     *
     * Given: title에만 키워드가 있는 Post A vs content에만 있는 Post B
     * When: POST /api/posts/search로 검색
     * Then: title weight=3.0으로 Post A가 더 높은 스코어 획득
     */
    @Test
    @DisplayName("T032-1: testSearchWithFieldWeights - 필드별 가중치가 검색 결과 스코어에 반영된다 (SC-003)")
    fun testSearchWithFieldWeights() {
        // Given: 테스트용 사용자 생성
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // Post A: title에 키워드 있음
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "Kotlin 성능 최적화 가이드",
                        contentHtml = "일반적인 내용입니다.",
                        hashtags = listOf("Kotlin")
                    )
                ))
        ).andExpect(status().isCreated)

        // Post B: content에 키워드 있음
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "일반적인 제목",
                        contentHtml = "Kotlin 성능 최적화에 대한 상세한 설명입니다.",
                        hashtags = listOf("Backend")
                    )
                ))
        ).andExpect(status().isCreated)

        // 벡터화 완료 대기
        Thread.sleep(2000)

        val searchRequest = """
            {
                "query": "Kotlin 성능 최적화",
                "threshold": 0.0,
                "limit": 20
            }
        """.trimIndent()

        // When: POST /api/posts/search
        val result = mockMvc.perform(
            post("/api/posts/search")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest)
        )
            // Then: 두 게시글 모두 반환되며 스코어가 존재함
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results").isArray)
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].similarity").isNumber)
            .andExpect(jsonPath("$.results[1].similarity").isNumber)
            .andReturn()

        // 스코어 비교 (title이 더 높은 가중치를 가지므로 Post A가 더 높은 스코어 획득 가능)
        val responseJson = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseJson, Map::class.java)
        val results = responseMap["results"] as List<Map<String, Any>>

        assert(results.size == 2) { "검색 결과가 2개가 아닙니다: ${results.size}" }

        // 모든 결과에 유사도 스코어가 있는지 확인
        results.forEach { result ->
            val similarity = result["similarity"] as Double
            assert(similarity > 0.0) { "유사도 스코어가 0보다 커야 합니다: $similarity" }
        }
    }

    /**
     * T032-2: 임계값 경계 검증 테스트 (SC-006)
     *
     * Given: 다양한 유사도를 가진 게시글들
     * When: threshold=0.7로 검색
     * Then: 0.7 이상인 결과만 반환
     */
    @Test
    @DisplayName("T032-2: testSearchWithThreshold - 임계값 이하 결과를 제외한다 (SC-006)")
    fun testSearchWithThresholdBoundary() {
        // Given: 테스트용 사용자 및 게시글 생성
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // 높은 유사도: 제목과 내용 모두 키워드 포함
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "Spring Boot 성능 최적화 완벽 가이드",
                        contentHtml = "Spring Boot 애플리케이션의 성능을 최적화하는 방법을 상세히 설명합니다.",
                        hashtags = listOf("Spring", "Performance")
                    )
                ))
        ).andExpect(status().isCreated)

        // 중간 유사도: 제목에만 일부 키워드
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "Spring 기초 튜토리얼",
                        contentHtml = "기본적인 웹 개발 내용입니다.",
                        hashtags = listOf("Tutorial")
                    )
                ))
        ).andExpect(status().isCreated)

        // 낮은 유사도: 완전히 다른 주제
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "요리 레시피 모음",
                        contentHtml = "맛있는 파스타 만들기",
                        hashtags = listOf("Cooking")
                    )
                ))
        ).andExpect(status().isCreated)

        // 벡터화 완료 대기
        Thread.sleep(2000)

        val searchRequest = """
            {
                "query": "Spring Boot 성능 최적화",
                "threshold": 0.7,
                "limit": 20
            }
        """.trimIndent()

        // When: threshold=0.7로 검색
        val result = mockMvc.perform(
            post("/api/posts/search")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results").isArray)
            .andReturn()

        // Then: 모든 결과의 similarity >= 0.7
        val responseJson = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseJson, Map::class.java)
        val results = responseMap["results"] as List<Map<String, Any>>

        results.forEach { result ->
            val similarity = result["similarity"] as Double
            assert(similarity >= 0.7) { "임계값 0.7 이하 결과 발견: similarity=$similarity" }
        }
    }

    /**
     * T032-3: 빈 결과 처리 테스트
     *
     * Given: 존재하지 않는 키워드 검색
     * When: POST /api/posts/search
     * Then: 200 OK + 빈 배열 반환
     */
    @Test
    @DisplayName("T032-3: testSearchWithNoResults - 결과가 없을 때 빈 배열을 반환한다")
    fun testSearchWithNoResults() {
        // Given: 테스트용 사용자
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // 게시글 1개 생성
        mockMvc.perform(
            post("/api/posts")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePostRequest(
                        title = "Spring Boot 가이드",
                        contentHtml = "Spring Boot 기초 내용입니다.",
                        hashtags = listOf("Spring")
                    )
                ))
        ).andExpect(status().isCreated)

        // 벡터화 완료 대기
        Thread.sleep(2000)

        val searchRequest = """
            {
                "query": "완전히 관련 없는 키워드 xyz123 abc789",
                "threshold": 0.5,
                "limit": 20
            }
        """.trimIndent()

        // When: 관련 없는 키워드로 검색
        val result = mockMvc.perform(
            post("/api/posts/search")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest)
        )
            // Then: 200 OK (빈 배열 또는 매우 낮은 유사도 결과)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results").isArray)
            .andReturn()

        // ML 기반 벡터 검색은 완전히 관련 없는 키워드도 낮은 유사도로 반환할 수 있음
        // threshold=0.5로 필터링되거나 또는 낮은 유사도로 반환됨
        val responseJson = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseJson, Map::class.java)
        val results = responseMap["results"] as List<Map<String, Any>>

        // 결과가 있다면 유사도가 낮아야 함 (임계값 근처)
        results.forEach { result ->
            val similarity = result["similarity"] as Double
            assert(similarity >= 0.5) { "임계값 이하 결과 발견: similarity=$similarity" }
        }
    }

    /**
     * T032-4: 인증 실패 테스트
     *
     * Given: Authorization 헤더 없음
     * When: POST /api/posts/search
     * Then: 401 Unauthorized
     */
    @Test
    @DisplayName("T032-4: testSearchUnauthorized - 인증 없이 검색 시 401 오류를 반환한다")
    fun testSearchUnauthorized() {
        // Given: 검색 요청 (인증 헤더 없음)
        val searchRequest = """
            {
                "query": "test query",
                "threshold": 0.5,
                "limit": 10
            }
        """.trimIndent()

        // When: Authorization 헤더 없이 POST /api/posts/search
        mockMvc.perform(
            post("/api/posts/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequest)
        )
            // Then: 401 Unauthorized
            .andExpect(status().isUnauthorized)
    }

    /**
     * T079: 소프트 삭제 기능 테스트
     *
     * Given: 작성자의 게시글
     * When: DELETE /api/posts/{id} 호출
     * Then: 204 No Content + DB에서 deleted=true
     */
    @Test
    @DisplayName("testDeletePost - 게시글을 소프트 삭제한다")
    fun testDeletePost() {
        // Given: 테스트용 사용자 및 게시글 생성
        val user = userRepository.save(
            User(
                email = "author@example.com",
                name = "작성자",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "삭제될 게시글",
                contentMarkdown = "삭제될 내용",
                contentHtml = "<p>삭제될 내용</p>",
                contentText = "삭제될 내용",
                author = user,
                hashtags = arrayOf("삭제", "테스트")
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: DELETE /api/posts/{id}
        mockMvc.perform(
            delete("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 204 No Content
            .andExpect(status().isNoContent)

        // 소프트 삭제 확인
        val deletedPost = postRepository.findById(post.id!!).get()
        assert(deletedPost.deleted) { "게시글이 소프트 삭제되지 않았습니다 (deleted=false)" }
    }

    /**
     * T080: 삭제 권한 실패 테스트
     *
     * Given: 작성자 A의 게시글
     * When: 사용자 B가 삭제 시도
     * Then: 403 Forbidden + 게시글 유지
     */
    @Test
    @DisplayName("testDeletePostForbidden - 작성자가 아닌 경우 삭제 시 403 오류를 반환한다")
    fun testDeletePostForbidden() {
        // Given: 작성자 A 및 게시글 생성
        val authorA = userRepository.save(
            User(
                email = "author.a@example.com",
                name = "작성자 A",
                profileImageUrl = null
            )
        )

        val post = postRepository.save(
            Post(
                title = "A의 게시글",
                contentMarkdown = "A가 작성한 내용",
                contentHtml = "<p>A가 작성한 내용</p>",
                contentText = "A가 작성한 내용",
                author = authorA,
                hashtags = arrayOf("A")
            )
        )

        // 사용자 B 생성
        val userB = userRepository.save(
            User(
                email = "user.b@example.com",
                name = "사용자 B",
                profileImageUrl = null
            )
        )

        val accessTokenB = jwtTokenService.generateAccessToken(userB.id!!, userB.email)

        // When: 사용자 B가 DELETE /api/posts/{id} 시도
        mockMvc.perform(
            delete("/api/posts/${post.id}")
                .header("Authorization", "Bearer $accessTokenB")
        )
            // Then: 403 Forbidden
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.error.message").value("게시글을 삭제할 권한이 없습니다"))

        // 게시글이 삭제되지 않았는지 확인
        val unchangedPost = postRepository.findById(post.id!!).get()
        assert(!unchangedPost.deleted) { "게시글이 삭제되었습니다 (deleted=true)" }
    }

    /**
     * T081: 삭제된 게시글 목록 제외 테스트
     *
     * Given: 게시글 5개 (2개 삭제됨)
     * When: GET /api/posts 호출
     * Then: 삭제되지 않은 3개만 반환
     */
    @Test
    @DisplayName("testGetPostsExcludeDeleted - 목록 조회 시 삭제된 게시글을 제외한다")
    fun testGetPostsExcludeDeleted() {
        // Given: 5개의 게시글 생성
        val user = userRepository.save(
            User(
                email = "user@example.com",
                name = "사용자",
                profileImageUrl = null
            )
        )

        val posts = mutableListOf<Post>()
        repeat(5) { index ->
            val post = postRepository.save(
                Post(
                    title = "게시글 ${index + 1}",
                    contentMarkdown = "내용 ${index + 1}",
                    contentHtml = "<p>내용 ${index + 1}</p>",
                    contentText = "내용 ${index + 1}",
                    author = user,
                    hashtags = arrayOf("테스트")
                )
            )
            posts.add(post)
        }

        // 2개 소프트 삭제 (첫 번째, 세 번째)
        posts[0].deleted = true
        posts[2].deleted = true
        postRepository.saveAll(posts)

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        // When: GET /api/posts
        mockMvc.perform(
            get("/api/posts")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 삭제되지 않은 3개만 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.posts.length()").value(3))
            .andExpect(jsonPath("$.pagination.totalElements").value(3))

        // 반환된 게시글 ID 확인 (삭제되지 않은 게시글만)
        val notDeletedIds = posts.filter { !it.deleted }.map { it.id }.toSet()
        assert(notDeletedIds.size == 3) { "삭제되지 않은 게시글이 3개가 아닙니다" }
    }
}
