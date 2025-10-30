package me.muheun.moaspace.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.repository.PostEmbeddingRepository
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.service.JwtTokenService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

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
@AutoConfigureMockMvc
@Transactional
@Sql("/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var postEmbeddingRepository: PostEmbeddingRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    /**
     * T047: 게시글 생성 및 자동 벡터화 테스트
     *
     * 시나리오:
     * 1. 테스트용 사용자 생성 및 JWT 토큰 발급
     * 2. POST /api/posts로 게시글 생성 요청
     * 3. 201 Created 응답 확인
     * 4. PostEmbedding이 자동 생성되었는지 확인
     */
    @Test
    fun `should create post with automatic vectorization`() {
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
            contentMarkdown = "**Next.js 15**가 출시되었습니다.",
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
            .andExpect(jsonPath("$.contentMarkdown").value(createRequest.contentMarkdown))
            .andExpect(jsonPath("$.contentHtml").exists())
            .andExpect(jsonPath("$.author.id").value(user.id!!))
            .andExpect(jsonPath("$.author.name").value(user.name))
            .andExpect(jsonPath("$.hashtags[0]").value("Next.js"))
            .andExpect(jsonPath("$.hashtags[1]").value("React"))
            .andExpect(jsonPath("$.hashtags[2]").value("웹개발"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
            .andReturn()

        val responseJson = result.response.contentAsString
        val responseMap = objectMapper.readValue(responseJson, Map::class.java)
        val postId = (responseMap["id"] as Number).toLong()

        val post = postRepository.findById(postId).get()
        assert(!post.deleted) { "생성된 게시글이 삭제 상태입니다" }

        val embedding = postEmbeddingRepository.findByPost(post)
        assert(embedding.isPresent) { "PostEmbedding이 자동 생성되지 않았습니다" }
        assert(embedding.get().modelName == "multilingual-e5-base") { "모델명이 일치하지 않습니다" }
    }

    /**
     * T048: 게시글 조회 테스트
     *
     * 시나리오:
     * 1. 테스트용 게시글 생성
     * 2. GET /api/posts/{id}로 조회 요청
     * 3. 200 OK 응답 및 게시글 정보 확인
     */
    @Test
    fun `should get post by id`() {
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
            .andExpect(jsonPath("$.contentMarkdown").value(post.contentMarkdown))
            .andExpect(jsonPath("$.contentHtml").value(post.contentHtml))
            .andExpect(jsonPath("$.author.id").value(user.id!!))
            .andExpect(jsonPath("$.author.name").value(user.name))
            .andExpect(jsonPath("$.hashtags[0]").value("테스트"))
    }

    /**
     * T049: 게시글 수정 및 벡터 재생성 테스트
     *
     * 시나리오:
     * 1. 테스트용 게시글 생성 및 초기 벡터화
     * 2. PUT /api/posts/{id}로 수정 요청
     * 3. 200 OK 응답 확인
     * 4. PostEmbedding이 재생성되었는지 확인 (새로운 embedding)
     */
    @Test
    fun `should update post and regenerate vector`() {
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
            contentMarkdown = "수정된 내용입니다.",
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
            .andExpect(jsonPath("$.contentMarkdown").value("수정된 내용입니다."))
            .andExpect(jsonPath("$.contentHtml").exists())
            .andExpect(jsonPath("$.hashtags[0]").value("수정"))
            .andExpect(jsonPath("$.hashtags[1]").value("테스트"))

        val updatedPost = postRepository.findById(post.id!!).get()
        assert(updatedPost.title == "수정된 제목") { "제목이 수정되지 않았습니다" }
        assert(updatedPost.contentText == "수정된 내용입니다.") { "contentText가 수정되지 않았습니다" }

        val embedding = postEmbeddingRepository.findByPost(updatedPost)
        assert(embedding.isPresent) { "PostEmbedding이 재생성되지 않았습니다" }
    }

    /**
     * T049: 소유권 검증 테스트 (작성자가 아닌 사용자가 수정 시도)
     *
     * 시나리오:
     * 1. 작성자 A가 게시글 생성
     * 2. 사용자 B가 게시글 수정 시도
     * 3. 403 Forbidden 응답 확인
     */
    @Test
    fun `should return 403 when non-owner tries to update post`() {
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
            contentMarkdown = "B가 수정한 내용",
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
     * 시나리오:
     * 1. Authorization 헤더 없이 POST /api/posts 호출
     * 2. 401 Unauthorized 응답 확인
     */
    @Test
    fun `should return 401 when creating post without authentication`() {
        // Given: 게시글 생성 요청
        val createRequest = CreatePostRequest(
            title = "인증 없는 게시글",
            contentMarkdown = "내용",
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
     * 시나리오:
     * 1. 존재하지 않는 게시글 ID로 GET /api/posts/{id} 호출
     * 2. 404 Not Found 응답 확인
     */
    @Test
    fun `should return 404 when post does not exist`() {
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
     * 시나리오:
     * 1. 게시글 생성 후 소프트 삭제 (deleted=true)
     * 2. GET /api/posts/{id} 호출
     * 3. 404 Not Found 응답 확인 (삭제된 글은 조회 불가)
     */
    @Test
    fun `should return 404 when accessing deleted post`() {
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
     * T065: 페이지네이션 테스트 (다양한 페이지 크기)
     *
     * 시나리오:
     * 1. 30개의 게시글 생성
     * 2. page=0, size=10으로 조회 → 10개 반환
     * 3. page=1, size=10으로 조회 → 10개 반환
     * 4. page=2, size=10으로 조회 → 10개 반환
     * 5. totalElements=30, totalPages=3 확인
     */
    @Test
    fun `should return paginated posts with correct pagination info`() {
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
        val page0Result = mockMvc.perform(
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
     * 시나리오:
     * 1. #AI 태그 게시글 5개 생성
     * 2. #Backend 태그 게시글 3개 생성
     * 3. GET /api/posts?hashtag=AI → 5개 반환
     * 4. GET /api/posts?hashtag=Backend → 3개 반환
     * 5. GET /api/posts (필터 없음) → 8개 반환
     */
    @Test
    fun `should filter posts by hashtag`() {
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
     * T067: 벡터 검색 정확도 테스트 (임계값 필터링)
     *
     * 시나리오:
     * 1. "인공지능 기술의 발전" 게시글 생성
     * 2. "머신러닝과 딥러닝" 게시글 생성
     * 3. "요리 레시피" 게시글 생성
     * 4. POST /api/posts/search { query: "AI와 머신러닝", threshold: 0.6 }
     * 5. 유사한 게시글 (1, 2)만 반환, 무관한 게시글 (3) 제외
     * 6. 유사도 점수 포함 확인
     */
    @Test
    fun `should return similar posts with threshold filtering and similarity scores`() {
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
                        contentMarkdown = "인공지능과 머신러닝 기술이 빠르게 발전하고 있습니다.",
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
                        contentMarkdown = "머신러닝과 딥러닝은 AI의 핵심 기술입니다.",
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
                        contentMarkdown = "맛있는 파스타 만드는 법을 소개합니다.",
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
     * T079: 소프트 삭제 기능 테스트
     *
     * 시나리오:
     * 1. 작성자가 게시글 생성
     * 2. DELETE /api/posts/{id}로 삭제 요청
     * 3. 204 No Content 응답 확인
     * 4. DB에서 deleted=true 확인
     */
    @Test
    fun `should soft delete post successfully`() {
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
     * T080: 소유자가 아닌 경우 삭제 권한 실패 테스트
     *
     * 시나리오:
     * 1. 작성자 A가 게시글 생성
     * 2. 사용자 B가 DELETE /api/posts/{id} 시도
     * 3. 403 Forbidden 응답 확인
     * 4. 게시글이 삭제되지 않았는지 확인
     */
    @Test
    fun `should return 403 when non-owner tries to delete post`() {
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
     * T081: 삭제된 게시글이 목록 API에서 제외되는지 테스트
     *
     * 시나리오:
     * 1. 게시글 5개 생성
     * 2. 그 중 2개 소프트 삭제 (deleted=true)
     * 3. GET /api/posts 호출
     * 4. 삭제되지 않은 3개만 반환되는지 확인
     * 5. totalElements=3 확인
     */
    @Test
    fun `should exclude deleted posts from list API`() {
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
