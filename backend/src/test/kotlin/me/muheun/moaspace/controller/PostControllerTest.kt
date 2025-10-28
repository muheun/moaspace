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
            content = "<p><strong>Next.js 15</strong>가 출시되었습니다.</p>",
            plainContent = "Next.js 15가 출시되었습니다.",
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
            .andExpect(jsonPath("$.content").value(createRequest.content))
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
                content = "<p>테스트 내용</p>",
                plainContent = "테스트 내용",
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
            .andExpect(jsonPath("$.content").value(post.content))
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
                content = "<p>원본 내용</p>",
                plainContent = "원본 내용",
                author = user,
                hashtags = arrayOf("원본")
            )
        )

        val accessToken = jwtTokenService.generateAccessToken(user.id!!, user.email)

        val updateRequest = UpdatePostRequest(
            title = "수정된 제목",
            content = "<p>수정된 내용입니다.</p>",
            plainContent = "수정된 내용입니다.",
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
            .andExpect(jsonPath("$.content").value("<p>수정된 내용입니다.</p>"))
            .andExpect(jsonPath("$.hashtags[0]").value("수정"))
            .andExpect(jsonPath("$.hashtags[1]").value("테스트"))

        val updatedPost = postRepository.findById(post.id!!).get()
        assert(updatedPost.title == "수정된 제목") { "제목이 수정되지 않았습니다" }
        assert(updatedPost.plainContent == "수정된 내용입니다.") { "plainContent가 수정되지 않았습니다" }

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
                content = "<p>A가 작성한 내용</p>",
                plainContent = "A가 작성한 내용",
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
            content = "<p>B가 수정한 내용</p>",
            plainContent = "B가 수정한 내용",
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
            content = "<p>내용</p>",
            plainContent = "내용",
            hashtags = emptyList()
        )

        val requestJson = objectMapper.writeValueAsString(createRequest)

        // When: Authorization 헤더 없이 POST /api/posts
        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            // Then: 401 Unauthorized
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.error.message").value("인증이 필요합니다"))
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
                content = "<p>삭제될 내용</p>",
                plainContent = "삭제될 내용",
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
}
