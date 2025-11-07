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
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@Profile("test")
@EnableWebSecurity
class TestSecurityConfig(
    private val jwtTokenService: JwtTokenService
) {

    /**
     * 테스트용 Security Filter Chain
     *
     * JWT 인증만 활성화하고 OAuth2, CORS는 비활성화합니다.
     * 실제 환경과 동일한 JWT 검증 로직을 사용하여
     * 인증 테스트의 정확성을 보장합니다.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf { it.disable() }

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
                        "/api/auth/logout"
                    ).permitAll()

                    // 인증 필요 엔드포인트
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/posts/**").authenticated()

                    // 벡터 설정 API (인증 불필요 - 테스트용)
                    .requestMatchers("/api/vector-configs/**").permitAll()

                    // 나머지 API 엔드포인트 (기본 허용)
                    .requestMatchers("/api/**").permitAll()

                    // 나머지 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }

        return http.build()
    }

    /**
     * JWT Decoder 설정
     *
     * 실제 SecurityConfig와 동일한 JWT 검증 로직을 사용합니다.
     * jjwt 라이브러리를 사용하여 토큰을 파싱하고 검증합니다.
     *
     * 테스트에서 생성한 JWT 토큰이 올바르게 검증되도록
     * 동일한 Secret Key와 알고리즘을 사용합니다.
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
                // JWT 파싱/검증 실패 시 BadJwtException으로 변환 (401 Unauthorized 응답)
                throw org.springframework.security.oauth2.jwt.BadJwtException("Invalid JWT token: ${e.message}", e)
            }
        }
    }
}