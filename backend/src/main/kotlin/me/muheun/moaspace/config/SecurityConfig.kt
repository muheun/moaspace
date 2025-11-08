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

// Spring Security 설정 - OAuth2 + JWT 인증, CORS 설정
@Configuration
@Profile("!test")
@EnableWebSecurity
class SecurityConfig(
    private val oauth2SuccessHandler: me.muheun.moaspace.security.OAuth2SuccessHandler,
    private val oauth2FailureHandler: me.muheun.moaspace.security.OAuth2FailureHandler,
    private val restAuthenticationEntryPoint: me.muheun.moaspace.security.RestAuthenticationEntryPoint,
    private val jwtTokenService: JwtTokenService
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(restAuthenticationEntryPoint)
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/error",
                        "/api/health",
                        "/api/auth/logout",
                        "/login/**",
                        "/oauth2/**"
                    ).permitAll()
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/posts/**").authenticated()
                    .requestMatchers("/api/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .redirectionEndpoint { redirection ->
                        redirection.baseUri("/login/oauth2/code/*")
                    }
                    .successHandler(oauth2SuccessHandler)
                    .failureHandler(oauth2FailureHandler)
            }

        return http.build()
    }

    /**
     * JWT Decoder 설정
     *
     * jjwt 라이브러리로 토큰 생성 및 검증하여 Nimbus JWT와의 호환성 이슈 방지
     */
    @Bean
    fun jwtDecoder(): JwtDecoder {
        return JwtDecoder { token ->
            try {
                val secretKeyBytes = jwtTokenService.getSecretKeyBytes()
                val secretKey: SecretKey = SecretKeySpec(secretKeyBytes, "HmacSHA256")

                val claims = io.jsonwebtoken.Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload

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
                throw org.springframework.security.oauth2.jwt.BadJwtException("Invalid JWT token: ${e.message}", e)
            }
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
            )
            allowedMethods = listOf(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
            )
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
            )
            allowCredentials = true
            maxAge = 3600L
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
