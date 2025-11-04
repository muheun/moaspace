package me.muheun.moaspace.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(RestAuthenticationEntryPoint::class.java)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val requestUri = request.requestURI

        if (requestUri.startsWith("/api/")) {
            logger.warn("인증되지 않은 API 요청: $requestUri")

            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json;charset=UTF-8"

            val errorResponse = mapOf(
                "error" to mapOf(
                    "code" to "UNAUTHORIZED",
                    "message" to "인증이 필요합니다. 로그인해주세요.",
                    "timestamp" to LocalDateTime.now().toString()
                )
            )

            objectMapper.writeValue(response.writer, errorResponse)
            return
        }

        logger.info("브라우저 요청 인증 실패, OAuth2 로그인으로 리다이렉트: $requestUri")
        response.sendRedirect("/oauth2/authorization/google")
    }
}
