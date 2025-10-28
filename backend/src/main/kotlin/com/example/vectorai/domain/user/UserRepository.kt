package com.example.vectorai.domain.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * User 엔티티 Repository
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필수
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {
    /**
     * 이메일로 사용자 조회 (Google OAuth 로그인)
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * 이메일 존재 여부 확인
     */
    fun existsByEmail(email: String): Boolean
}
