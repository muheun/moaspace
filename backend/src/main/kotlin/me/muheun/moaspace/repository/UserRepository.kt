package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

/**
 * User 엔티티 Repository
 * Google OAuth 인증 사용자 관리
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     * @param email Google OAuth 이메일 주소
     * @return 사용자 (Optional)
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * 이메일 존재 여부 확인
     * @param email Google OAuth 이메일 주소
     * @return 존재 여부
     */
    fun existsByEmail(email: String): Boolean
}
