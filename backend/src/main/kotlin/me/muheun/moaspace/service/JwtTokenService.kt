package me.muheun.moaspace.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtTokenService(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.access-token-expiration:3600000}") private val accessTokenExpiration: Long = 3600000, // 1시간
    @Value("\${jwt.refresh-token-expiration:604800000}") private val refreshTokenExpiration: Long = 604800000 // 7일
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

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

    fun getUserIdFromToken(token: String): Long {
        val claims = parseToken(token)
        return claims.subject.toLong()
    }

    fun getEmailFromToken(token: String): String? {
        val claims = parseToken(token)
        return claims["email"] as? String
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun extractToken(bearerToken: String): String? {
        return if (bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    /**
     * SecurityConfig의 NimbusJwtDecoder와 동일한 키 공유
     */
    fun getSecretKeyBytes(): ByteArray {
        return jwtSecret.toByteArray(StandardCharsets.UTF_8)
    }
}
