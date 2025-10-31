package me.muheun.moaspace.controller

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.service.JwtTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

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
@ActiveProfiles("test")
@AutoConfigureMockMvc  // Security 필터 활성화 (TestSecurityConfig 사용)
@Transactional
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jwtTokenService: JwtTokenService

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 users 테이블 정리
        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    /**
     * T034: JWT 토큰 검증 테스트
     *
     * Given: 유효한 JWT 토큰
     * When: GET /api/auth/me 호출
     * Then: 200 OK + 사용자 정보 반환
     */
    @Test
    @DisplayName("testGetUserInfo - 유효한 JWT 토큰으로 사용자 정보를 조회한다")
    fun testGetUserInfo() {
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
     * Given: 잘못된 JWT 토큰
     * When: GET /api/auth/me 호출
     * Then: 401 Unauthorized 응답
     */
    @Test
    @DisplayName("testGetUserInfoInvalidToken - 잘못된 JWT 토큰 사용 시 401 오류를 반환한다")
    fun testGetUserInfoInvalidToken() {
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
     * Given: Authorization 헤더 없음
     * When: GET /api/auth/me 호출
     * Then: 401 Unauthorized 응답
     */
    @Test
    @DisplayName("testGetUserInfoNoAuth - Authorization 헤더 없이 호출 시 401 오류를 반환한다")
    fun testGetUserInfoNoAuth() {
        // When: GET /api/auth/me without Authorization header
        mockMvc.perform(get("/api/auth/me"))
            // Then: 401 Unauthorized (Spring Security 표준 응답)
            .andExpect(status().isUnauthorized)
    }

    /**
     * T034: 존재하지 않는 사용자 ID로 404 오류
     *
     * Given: 존재하지 않는 사용자 ID의 JWT 토큰
     * When: GET /api/auth/me 호출
     * Then: 404 Not Found 응답
     */
    @Test
    @DisplayName("testGetUserInfoNotFound - 존재하지 않는 사용자 조회 시 404 오류를 반환한다")
    fun testGetUserInfoNotFound() {
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
     * T032: 로그아웃 성공
     *
     * Given: 로그아웃 요청
     * When: POST /api/auth/logout 호출
     * Then: 204 No Content 응답
     *
     * 참고: Stateless JWT 방식이므로 서버 측 특별 처리 없음
     */
    @Test
    @DisplayName("testLogout - 로그아웃 시 204 No Content를 반환한다")
    fun testLogout() {
        // When: POST /api/auth/logout
        mockMvc.perform(post("/api/auth/logout"))
            // Then: 204 No Content
            .andExpect(status().isNoContent)
    }

    /**
     * T033: OAuth 플로우 통합 테스트
     *
     * Given: OAuth2로 새 사용자 생성
     * When: 발급된 JWT 토큰으로 /api/auth/me 호출
     * Then: 200 OK + 사용자 정보 반환
     */
    @Test
    @DisplayName("testOAuthFlow - OAuth 인증 후 JWT 토큰 발급 및 사용자 정보 조회가 정상 동작한다")
    fun testOAuthFlow() {
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
     * Given: 생성된 JWT 액세스 토큰
     * When: 토큰에서 사용자 정보 추출
     * Then: 사용자 ID, 이메일 정보 일치 + 토큰 유효성 확인
     */
    @Test
    @DisplayName("testExtractUserFromToken - JWT 토큰에서 사용자 정보를 정확히 추출한다")
    fun testExtractUserFromToken() {
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
