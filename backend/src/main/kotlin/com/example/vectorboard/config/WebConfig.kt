package com.example.vectorboard.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 * 프론트엔드(localhost:3000, localhost:3001 등)에서 백엔드 API 호출 허용
 */
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
}
