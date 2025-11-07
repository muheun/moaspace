package me.muheun.moaspace.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.service.JwtTokenService
import me.muheun.moaspace.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2SuccessHandler(
    private val userService: UserService,
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

        // 사용자 생성 또는 조회 (벡터화 포함)
        val user = userService.findOrCreateUser(email, name, profileImageUrl)

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

}
