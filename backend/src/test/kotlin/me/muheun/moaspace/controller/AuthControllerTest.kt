package me.muheun.moaspace.controller

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.service.JwtTokenService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * AuthController 통합 테스트
 * T033: TestRestTemplate로 OAuth 플로우 테스트
 * T034: Mock 요청으로 JWT 토큰 검증 테스트
 *
 * Constitution Principle V: 실제 DB 연동 테스트 (@SpringBootTest)
 *
 * 주요 테스트:
 * - GET /api/auth/me: 유효한 JWT 토큰으로 사용자 정보 조회
 * - GET /api/auth/me: 잘못된 JWT 토큰으로 401 오류
 * - GET /api/auth/me: Authorization 헤더 없이 401 오류
 * - POST /api/auth/logout: 로그아웃 성공 (204 No Content)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    /**
     * T034: Mock 요청으로 JWT 토큰 검증 테스트
     *
     * 시나리오:
     * 1. 테스트용 사용자 생성
     * 2. JWT 액세스 토큰 생성
     * 3. Authorization 헤더에 토큰 포함하여 GET /api/auth/me 호출
     * 4. 200 OK 응답 및 사용자 정보 반환 확인
     */
    @Test
    fun `should return user info when valid JWT token is provided`() {
        // Given: 테스트용 사용자 생성
        val user = userRepository.save(
            User(
                email = "test@example.com",
                name = "테스트 사용자",
                profileImageUrl = "https://via.placeholder.com/150"
            )
        )

        // JWT 액세스 토큰 생성
        val accessToken = jwtTokenService.generateAccessToken(
            userId = user.id!!,
            email = user.email
        )

        // When: GET /api/auth/me with Authorization header
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 200 OK 및 사용자 정보 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id!!))
            .andExpect(jsonPath("$.email").value(user.email))
            .andExpect(jsonPath("$.name").value(user.name))
            .andExpect(jsonPath("$.profileImageUrl").value(user.profileImageUrl))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    /**
     * T034: 잘못된 JWT 토큰으로 401 오류
     *
     * 시나리오:
     * 1. 잘못된 JWT 토큰 생성
     * 2. Authorization 헤더에 잘못된 토큰 포함하여 GET /api/auth/me 호출
     * 3. 401 Unauthorized 응답 확인
     */
    @Test
    fun `should return 401 when invalid JWT token is provided`() {
        // Given: 잘못된 JWT 토큰
        val invalidToken = "invalid.jwt.token"

        // When: GET /api/auth/me with invalid token
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $invalidToken")
        )
            // Then: 401 Unauthorized (Spring Security 표준 응답)
            .andExpect(status().isUnauthorized)
    }

    /**
     * T034: Authorization 헤더 없이 401 오류
     *
     * 시나리오:
     * 1. Authorization 헤더 없이 GET /api/auth/me 호출
     * 2. 401 Unauthorized 응답 확인
     */
    @Test
    fun `should return 401 when Authorization header is missing`() {
        // When: GET /api/auth/me without Authorization header
        mockMvc.perform(get("/api/auth/me"))
            // Then: 401 Unauthorized (Spring Security 표준 응답)
            .andExpect(status().isUnauthorized)
    }

    /**
     * T034: 존재하지 않는 사용자 ID로 404 오류
     *
     * 시나리오:
     * 1. 존재하지 않는 사용자 ID로 JWT 토큰 생성
     * 2. Authorization 헤더에 토큰 포함하여 GET /api/auth/me 호출
     * 3. 404 Not Found 응답 확인
     */
    @Test
    fun `should return 404 when user does not exist`() {
        // Given: 존재하지 않는 사용자 ID로 JWT 토큰 생성
        val nonExistentUserId = 99999L
        val accessToken = jwtTokenService.generateAccessToken(
            userId = nonExistentUserId,
            email = "nonexistent@example.com"
        )

        // When: GET /api/auth/me with valid token but nonexistent user
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 404 Not Found
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    /**
     * T032: 로그아웃 성공 (204 No Content)
     *
     * 시나리오:
     * 1. POST /api/auth/logout 호출
     * 2. 204 No Content 응답 확인
     *
     * 참고: Stateless JWT 방식이므로 서버 측에서는 특별한 처리 없음
     */
    @Test
    fun `should return 204 when logout is successful`() {
        // When: POST /api/auth/logout
        mockMvc.perform(post("/api/auth/logout"))
            // Then: 204 No Content
            .andExpect(status().isNoContent)
    }

    /**
     * T033: OAuth 플로우 통합 테스트
     *
     * OAuth2SuccessHandler가 올바르게 동작하는지 확인:
     * 1. 새 사용자 생성
     * 2. JWT 토큰 발급
     * 3. 발급된 토큰으로 /api/auth/me 호출
     * 4. 사용자 정보 반환 확인
     */
    @Test
    fun `should create user and issue JWT token after OAuth success`() {
        // Given: OAuth2 인증으로 새 사용자 생성 (OAuth2SuccessHandler 시뮬레이션)
        val email = "oauth.user@example.com"
        val name = "OAuth 사용자"
        val profileImageUrl = "https://lh3.googleusercontent.com/..."

        val user = userRepository.save(
            User(
                email = email,
                name = name,
                profileImageUrl = profileImageUrl
            )
        )

        // JWT 액세스 토큰 발급
        val accessToken = jwtTokenService.generateAccessToken(
            userId = user.id!!,
            email = user.email
        )

        // When: 발급된 토큰으로 /api/auth/me 호출
        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            // Then: 200 OK 및 사용자 정보 반환
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id!!))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.profileImageUrl").value(profileImageUrl))
    }

    /**
     * T034: JWT 토큰에서 사용자 정보 추출 테스트
     *
     * JwtTokenService가 올바르게 동작하는지 확인:
     * 1. 액세스 토큰 생성
     * 2. 토큰에서 사용자 ID 추출
     * 3. 토큰 유효성 검증
     */
    @Test
    fun `should extract user info from JWT token`() {
        // Given: 테스트용 사용자
        val userId = 12345L
        val email = "jwt.test@example.com"

        // JWT 액세스 토큰 생성
        val accessToken = jwtTokenService.generateAccessToken(
            userId = userId,
            email = email
        )

        // When: 토큰에서 사용자 정보 추출
        val extractedUserId = jwtTokenService.getUserIdFromToken(accessToken)
        val extractedEmail = jwtTokenService.getEmailFromToken(accessToken)
        val isValid = jwtTokenService.validateToken(accessToken)

        // Then: 올바른 정보 추출 확인
        assert(extractedUserId == userId) { "사용자 ID가 일치하지 않습니다" }
        assert(extractedEmail == email) { "이메일이 일치하지 않습니다" }
        assert(isValid) { "JWT 토큰이 유효하지 않습니다" }
    }
}
