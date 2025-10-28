package com.example.vectorai.domain.user

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사용자 엔티티 (Google OAuth 인증)
 *
 * Constitution Principle V: 실제 DB 연동 테스트 대상
 */
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false, length = 255)
    val email: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val profileImageUrl: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
