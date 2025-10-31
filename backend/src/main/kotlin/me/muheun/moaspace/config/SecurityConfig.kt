package me.muheun.moaspace.config

import me.muheun.moaspace.service.JwtTokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Spring Security 설정
 * T018: Google용 Spring Security OAuth2 Client 설정
 * T020: 프론트엔드 origin (localhost:3000) 허용을 위한 CORS 설정
 * T029: OAuth2SuccessHandler 등록
 *
 * 주요 기능:
 * - Google OAuth2 로그인 설정
 * - JWT 기반 인증 (Stateless) - Spring Security Filter Chain 자동 검증
 * - OAuth2 Resource Server (NimbusJwtDecoder)
 * - CORS 설정 (localhost:3000 허용)
 * - 공개 엔드포인트 허용 (인증 불필요)
 *
 * @Profile("!test"): 테스트 환경에서는 Security 설정 비활성화
 */
@Configuration
@Profile("!test")
@EnableWebSecurity
class SecurityConfig(
    private val oauth2SuccessHandler: me.muheun.moaspace.security.OAuth2SuccessHandler,
    private val jwtTokenService: JwtTokenService
) {

    /**
     * Security Filter Chain 설정
     * - OAuth2 로그인 활성화
     * - OAuth2 Resource Server (JWT 자동 검증)
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

            // OAuth2 Resource Server 설정 (JWT 자동 검증)
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            }

            // URL별 인증 설정
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트 (인증 불필요)
                    .requestMatchers(
                        "/",
                        "/error",
                        "/api/health",
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/login/**",
                        "/oauth2/**"
                    ).permitAll()

                    // 인증 필요 엔드포인트
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/posts/**").authenticated()

                    // API 엔드포인트 (기본 허용)
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
     * JWT Decoder 설정
     * JwtTokenService의 jjwt 파서를 활용한 Custom JwtDecoder
     *
     * JwtTokenService는 jjwt (io.jsonwebtoken) 라이브러리로 토큰을 생성하므로,
     * 동일한 라이브러리로 검증하는 Custom Decoder를 사용합니다.
     *
     * Nimbus JWT 대신 jjwt 파서를 사용하여 호환성을 보장합니다.
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        return JwtDecoder { token ->
            try {
                // jjwt 라이브러리로 토큰 검증 및 파싱
                val secretKeyBytes = jwtTokenService.getSecretKeyBytes()
                val secretKey: SecretKey = SecretKeySpec(secretKeyBytes, "HmacSHA256")

                val claims = io.jsonwebtoken.Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload

                // Spring Security Jwt 객체로 변환
                val headers = mutableMapOf<String, Any>("alg" to "HS256", "typ" to "JWT")
                val claimsMap = claims.mapValues { it.value as Any }.toMutableMap()

                org.springframework.security.oauth2.jwt.Jwt(
                    token,
                    claims.issuedAt.toInstant(),
                    claims.expiration.toInstant(),
                    headers,
                    claimsMap
                )
            } catch (e: Exception) {
                // JWT 파싱/검증 실패 시 BadCredentialsException으로 변환 (401 Unauthorized 응답)
                throw org.springframework.security.oauth2.jwt.BadJwtException("Invalid JWT token: ${e.message}", e)
            }
        }
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
