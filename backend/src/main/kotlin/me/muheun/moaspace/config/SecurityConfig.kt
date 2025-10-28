package me.muheun.moaspace.config

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
 * T018: Google용 Spring Security OAuth2 Client 설정
 * T020: 프론트엔드 origin (localhost:3000) 허용을 위한 CORS 설정
 * T029: OAuth2SuccessHandler 등록
 *
 * 주요 기능:
 * - Google OAuth2 로그인 설정
 * - JWT 기반 인증 (Stateless)
 * - CORS 설정 (localhost:3000 허용)
 * - 공개 엔드포인트 허용 (인증 불필요)
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oauth2SuccessHandler: me.muheun.moaspace.security.OAuth2SuccessHandler
) {

    /**
     * Security Filter Chain 설정
     * - OAuth2 로그인 활성화
     * - JWT 기반 세션 비활성화 (Stateless)
     * - CORS 설정 적용
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF 비활성화 (JWT 사용으로 인해 필요 없음)
            .csrf { it.disable() }

            // CORS 설정 활성화
            .cors { it.configurationSource(corsConfigurationSource()) }

            // 세션 관리 - Stateless (JWT 사용)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // URL별 인증 설정
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트 (인증 불필요)
                    .requestMatchers(
                        "/",
                        "/error",
                        "/api/health",
                        "/api/auth/**",
                        "/login/**",
                        "/oauth2/**"
                    ).permitAll()

                    // API 엔드포인트 (JWT 인증은 컨트롤러에서 처리)
                    .requestMatchers("/api/**").permitAll()

                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }

            // OAuth2 로그인 설정
            .oauth2Login { oauth2 ->
                oauth2
                    // OAuth2 로그인 페이지 경로 설정
                    .loginPage("/api/auth/login")

                    // OAuth2 인증 후 리다이렉트 엔드포인트
                    .redirectionEndpoint { redirection ->
                        redirection.baseUri("/login/oauth2/code/*")
                    }

                    // OAuth2 인증 성공 핸들러 등록 (T029)
                    .successHandler(oauth2SuccessHandler)
            }

        return http.build()
    }

    /**
     * CORS 설정
     * T020: 프론트엔드 origin (localhost:3000) 허용
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 허용할 Origin (프론트엔드 주소)
            allowedOrigins = listOf(
                "http://localhost:3000",  // Next.js 개발 서버
                "http://127.0.0.1:3000"
            )

            // 허용할 HTTP 메서드
            allowedMethods = listOf(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )

            // 허용할 헤더
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
            )

            // 인증 정보 포함 허용 (쿠키, Authorization 헤더)
            allowCredentials = true

            // Pre-flight 요청 캐시 시간 (1시간)
            maxAge = 3600L

            // 응답 헤더 노출
            exposedHeaders = listOf(
                "Authorization",
                "Content-Type"
            )
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
