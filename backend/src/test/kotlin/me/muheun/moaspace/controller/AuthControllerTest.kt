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

    @Test
    @DisplayName("유효한 JWT 토큰으로 사용자 정보를 조회한다")
    fun testGetUserInfo() {
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

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id!!))
            .andExpect(jsonPath("$.email").value(user.email))
            .andExpect(jsonPath("$.name").value(user.name))
            .andExpect(jsonPath("$.profileImageUrl").value(user.profileImageUrl))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 사용 시 401 오류를 반환한다")
    fun testGetUserInfoInvalidToken() {
        val invalidToken = "invalid.jwt.token"

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $invalidToken")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Authorization 헤더 없이 호출 시 401 오류를 반환한다")
    fun testGetUserInfoNoAuth() {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 404 오류를 반환한다")
    fun testGetUserInfoNotFound() {
        val nonExistentUserId = 99999L
        val accessToken = jwtTokenService.generateAccessToken(
            userId = nonExistentUserId,
            email = "nonexistent@example.com"
        )

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    @Test
    @DisplayName("로그아웃 시 204 No Content를 반환한다")
    fun testLogout() {
        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("OAuth 인증 후 JWT 토큰 발급 및 사용자 정보 조회가 정상 동작한다")
    fun testOAuthFlow() {
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

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id!!))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.profileImageUrl").value(profileImageUrl))
    }

    @Test
    @DisplayName("JWT 토큰에서 사용자 정보를 정확히 추출한다")
    fun testExtractUserFromToken() {
        val userId = 12345L
        val email = "jwt.test@example.com"

        // JWT 액세스 토큰 생성
        val accessToken = jwtTokenService.generateAccessToken(
            userId = userId,
            email = email
        )

        val extractedUserId = jwtTokenService.getUserIdFromToken(accessToken)
        val extractedEmail = jwtTokenService.getEmailFromToken(accessToken)
        val isValid = jwtTokenService.validateToken(accessToken)

        assert(extractedUserId == userId) { "사용자 ID가 일치하지 않습니다" }
        assert(extractedEmail == email) { "이메일이 일치하지 않습니다" }
        assert(isValid) { "JWT 토큰이 유효하지 않습니다" }
    }
}
