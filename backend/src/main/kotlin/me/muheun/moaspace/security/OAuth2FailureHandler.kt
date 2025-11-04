package me.muheun.moaspace.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

/**
 * OAuth2 인증 실패 핸들러
 *
 * Google OAuth 인증 실패 시 다음 작업 수행:
 * 1. 에러 로그 기록
 * 2. 사용자 친화적 에러 메시지 생성
 * 3. 프론트엔드 콜백 페이지로 리다이렉트 (에러 정보 포함)
 *
 * OAuth2 표준 에러 처리 흐름:
 * - Backend 에러 발생 → OAuth2FailureHandler 캡처
 * - /callback?error=authentication_failed&message=... 로 리다이렉트
 * - Frontend에서 에러 메시지 표시
 *
 * ❌ 잘못된 방법: Spring 기본 에러 페이지 노출 (500 스택 트레이스 노출)
 * ✅ 올바른 방법: 사용자 친화적 메시지로 Frontend 리다이렉트
 */
@Component
class OAuth2FailureHandler(
    @Value("\${frontend.url:http://localhost:3000}") private val frontendUrl: String
) : SimpleUrlAuthenticationFailureHandler() {

    private val logger = LoggerFactory.getLogger(OAuth2FailureHandler::class.java)

    /**
     * OAuth2 인증 실패 시 호출
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param exception 인증 예외 (AuthenticationException)
     */
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        logger.error("OAuth2 인증 실패: ${exception.message}", exception)

        // 프론트엔드 콜백 페이지로 리다이렉트 (에러 정보 포함)
        val targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/callback")
            .queryParam("error", "authentication_failed")
            .queryParam("message", "로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            .build()
            .toUriString()

        logger.info("프론트엔드 에러 페이지로 리다이렉트: $targetUrl")

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
