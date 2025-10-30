package me.muheun.moaspace.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성 및 검증 서비스
 * T019: JWT 토큰 생성 서비스 구현
 *
 * 주요 기능:
 * - 액세스 토큰 생성 (1시간 유효)
 * - 리프레시 토큰 생성 (7일 유효)
 * - 토큰 검증 및 파싱
 * - 사용자 ID 추출
 */
@Service
class JwtTokenService(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.access-token-expiration:3600000}") private val accessTokenExpiration: Long = 3600000, // 1시간
    @Value("\${jwt.refresh-token-expiration:604800000}") private val refreshTokenExpiration: Long = 604800000 // 7일
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * 액세스 토큰 생성
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @return JWT 액세스 토큰
     */
    fun generateAccessToken(userId: Long, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    /**
     * 리프레시 토큰 생성
     * @param userId 사용자 ID
     * @return JWT 리프레시 토큰
     */
    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    /**
     * 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    fun getUserIdFromToken(token: String): Long {
        val claims = parseToken(token)
        return claims.subject.toLong()
    }

    /**
     * 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 사용자 이메일 (액세스 토큰만 해당)
     */
    fun getEmailFromToken(token: String): String? {
        val claims = parseToken(token)
        return claims["email"] as? String
    }

    /**
     * 토큰 유효성 검증
     * @param token JWT 토큰
     * @return 유효 여부
     */
    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 토큰 파싱
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * Bearer 토큰에서 순수 JWT 추출
     * @param bearerToken "Bearer {token}" 형식
     * @return JWT 토큰
     */
    fun extractToken(bearerToken: String): String? {
        return if (bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    /**
     * SecretKey 바이트 배열 반환
     * SecurityConfig의 NimbusJwtDecoder와 동일한 키 공유
     * @return SecretKey 바이트 배열
     */
    fun getSecretKeyBytes(): ByteArray {
        return jwtSecret.toByteArray(StandardCharsets.UTF_8)
    }
}
