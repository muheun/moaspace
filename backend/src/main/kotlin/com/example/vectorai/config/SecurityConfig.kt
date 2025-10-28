package com.example.vectorai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
 *
 * - Google OAuth2 Client 설정
 * - CORS 설정 (프론트엔드 localhost:3000 허용)
 * - JWT 기반 stateless 인증
 *
 * Constitution Principle V: 실제 DB 연동 테스트 대상
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers(
                        "/",
                        "/api/auth/**",
                        "/oauth2/**",
                        "/login/**",
                        "/error"
                    ).permitAll()
                    // Protected endpoints
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/api/auth/callback", true)
                    // OAuth2SuccessHandler는 User Story 1에서 구현 (T029)
            }

        return http.build()
    }

    /**
     * CORS 설정 (Constitution: 프론트엔드 localhost:3000 허용)
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // 허용할 Origin (프론트엔드 URL)
        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        )

        // 허용할 HTTP 메서드
        configuration.allowedMethods = listOf(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        )

        // 허용할 헤더
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        )

        // 인증 정보 (쿠키, Authorization 헤더 등) 포함 허용
        configuration.allowCredentials = true

        // Preflight 요청 캐시 시간 (1시간)
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
