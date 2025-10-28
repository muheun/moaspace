package com.example.vectorai.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

/**
 * JWT 토큰 생성 및 검증 서비스
 *
 * Constitution Principle V: 실제 DB 연동 테스트 대상
 */
@Service
class JwtTokenService(
    @Value("\${jwt.secret:your-256-bit-secret-key-here-must-be-at-least-256-bits-long-for-hs256}")
    private val secretKey: String,

    @Value("\${jwt.expiration:86400000}") // 기본 24시간 (밀리초)
    private val expirationMs: Long
) {

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    /**
     * JWT 토큰 생성 (사용자 ID와 이메일 포함)
     */
    fun generateToken(userId: Long, email: String): String {
        val now = Date()
        val expirationDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("email", email)
            .setIssuedAt(now)
            .setExpiration(expirationDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    fun getUserIdFromToken(token: String): Long {
        val claims = getClaims(token)
        return claims.subject.toLong()
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    fun getEmailFromToken(token: String): String {
        val claims = getClaims(token)
        return claims["email"] as String
    }

    /**
     * JWT 토큰 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            claims.expiration.after(Date())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * JWT 토큰에서 Claims 추출
     */
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key as javax.crypto.SecretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
