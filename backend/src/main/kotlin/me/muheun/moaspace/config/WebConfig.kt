package me.muheun.moaspace.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.UUID

// CORS 및 요청 추적(MDC) 설정
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")  // /api/** 경로에 대해 CORS 허용
            .allowedOriginPatterns("*")  // 모든 origin 허용 (credentials와 함께 사용 가능)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 허용 HTTP 메서드
            .allowedHeaders("*")  // 모든 헤더 허용
            .allowCredentials(true)  // 인증 정보 포함 허용
            .maxAge(3600)  // preflight 요청 캐시 시간 (1시간)
    }

    // 요청 추적용 MDC 필터 - requestId, remoteIp, userAgent, method, uri 자동 설정
    @Bean
    fun mdcFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            try {
                // 요청 고유 ID 생성 (UUID 앞 8자리만 사용)
                val requestId = UUID.randomUUID().toString().substring(0, 8)

                // MDC에 컨텍스트 정보 설정
                MDC.put("requestId", requestId)
                MDC.put("remoteIp", getClientIp(request))
                MDC.put("userAgent", request.getHeader("User-Agent") ?: "Unknown")
                MDC.put("method", request.method)
                MDC.put("uri", request.requestURI)

                // 응답 헤더에도 requestId 추가 (클라이언트가 추적 가능)
                response.setHeader("X-Request-ID", requestId)

                filterChain.doFilter(request, response)
            } finally {
                // 요청 처리 완료 후 MDC 정리 (메모리 누수 방지)
                MDC.clear()
            }
        }

        // 프록시 환경에서 실제 클라이언트 IP 추출 (X-Forwarded-For 우선)
        private fun getClientIp(request: HttpServletRequest): String {
            val xForwardedFor = request.getHeader("X-Forwarded-For")
            return if (!xForwardedFor.isNullOrBlank()) {
                xForwardedFor.split(",")[0].trim()
            } else {
                request.remoteAddr ?: "Unknown"
            }
        }
    }
}
