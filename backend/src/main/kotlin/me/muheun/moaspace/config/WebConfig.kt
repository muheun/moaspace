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

/**
 * Web MVC 설정 (CORS, MDC 필터 등)
 *
 * - CORS: 프론트엔드에서 백엔드 API 호출 허용
 * - MDC 필터: 요청별 고유 ID 및 메타정보 추적 (로그 추적 용이)
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    /**
     * CORS 설정
     * 프론트엔드(localhost:3000, localhost:3001 등)에서 백엔드 API 호출 허용
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")  // /api/** 경로에 대해 CORS 허용
            .allowedOriginPatterns("*")  // 모든 origin 허용 (credentials와 함께 사용 가능)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 허용 HTTP 메서드
            .allowedHeaders("*")  // 모든 헤더 허용
            .allowCredentials(true)  // 인증 정보 포함 허용
            .maxAge(3600)  // preflight 요청 캐시 시간 (1시간)
    }

    /**
     * MDC(Mapped Diagnostic Context) 필터 등록
     *
     * 모든 HTTP 요청에 대해 고유 ID를 생성하고 MDC에 저장하여,
     * 로그 추적 및 분산 트레이싱을 지원합니다.
     *
     * **MDC 컨텍스트 키**:
     * - `requestId`: 요청 고유 ID (UUID)
     * - `remoteIp`: 클라이언트 IP 주소
     * - `userAgent`: User-Agent 헤더
     * - `method`: HTTP 메서드 (GET, POST 등)
     * - `uri`: 요청 URI
     */
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

        /**
         * 클라이언트 실제 IP 주소 추출
         *
         * 프록시/로드 밸런서를 거쳐온 경우 X-Forwarded-For 헤더에서 추출
         */
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
