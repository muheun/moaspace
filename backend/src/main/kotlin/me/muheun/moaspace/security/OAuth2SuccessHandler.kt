package me.muheun.moaspace.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.service.JwtTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

/**
 * OAuth2 인증 성공 핸들러
 * T029: User 생성 및 JWT 발급을 위한 OAuth2SuccessHandler 구현
 *
 * Google OAuth 인증 성공 후 다음 작업 수행:
 * 1. OAuth2User에서 사용자 정보 추출 (email, name, picture)
 * 2. DB에 사용자 존재 확인 (findByEmail)
 * 3. 없으면 새 사용자 생성, 있으면 기존 사용자 사용
 * 4. JWT 액세스 토큰 생성
 * 5. 프론트엔드로 리다이렉트 (쿼리 파라미터로 토큰 전달)
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필요
 */
@Component
class OAuth2SuccessHandler(
    private val userRepository: UserRepository,
    private val jwtTokenService: JwtTokenService,
    @Value("\${frontend.url:http://localhost:3000}") private val frontendUrl: String
) : SimpleUrlAuthenticationSuccessHandler() {

    private val logger = LoggerFactory.getLogger(OAuth2SuccessHandler::class.java)

    /**
     * OAuth2 인증 성공 시 호출
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication 인증 정보 (OAuth2User 포함)
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User

        // Google OAuth2에서 사용자 정보 추출
        val email = oauth2User.getAttribute<String>("email")
            ?: throw IllegalStateException("이메일 정보를 가져올 수 없습니다")

        val name = oauth2User.getAttribute<String>("name")
            ?: email.substringBefore("@") // 이름이 없으면 이메일에서 추출

        val profileImageUrl = oauth2User.getAttribute<String>("picture")

        logger.info("OAuth2 인증 성공: email=$email, name=$name")

        // 사용자 생성 또는 조회
        val user = findOrCreateUser(email, name, profileImageUrl)

        // JWT 액세스 토큰 생성
        val accessToken = jwtTokenService.generateAccessToken(
            userId = user.id!!,
            email = user.email
        )

        logger.info("JWT 토큰 발급 완료: userId=${user.id}")

        // 프론트엔드 콜백 페이지로 리다이렉트 (JWT 토큰 포함)
        val targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/callback")
            .queryParam("token", accessToken)
            .build()
            .toUriString()

        logger.info("프론트엔드로 리다이렉트: $targetUrl")

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    /**
     * 사용자 조회 또는 생성
     *
     * 이메일로 사용자 조회 → 없으면 새로 생성
     *
     * @param email Google OAuth 이메일
     * @param name 사용자 이름
     * @param profileImageUrl 프로필 이미지 URL
     * @return User 엔티티 (id 포함)
     */
    private fun findOrCreateUser(
        email: String,
        name: String,
        profileImageUrl: String?
    ): User {
        return userRepository.findByEmail(email).orElseGet {
            val newUser = User(
                email = email,
                name = name,
                profileImageUrl = profileImageUrl
            )
            val savedUser = userRepository.save(newUser)
            logger.info("신규 사용자 생성: userId=${savedUser.id}, email=$email")
            savedUser
        }
    }
}
