package me.muheun.moaspace.service

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * User 서비스
 * T030: 사용자 CRUD 작업을 위한 UserService 생성
 *
 * 주요 기능:
 * - 사용자 조회 (ID, 이메일)
 * - 사용자 생성
 * - 사용자 존재 여부 확인
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필요
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * 사용자 ID로 조회
     *
     * @param userId 사용자 ID
     * @return User 엔티티
     * @throws NoSuchElementException 사용자가 존재하지 않을 경우
     */
    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다: userId=$userId") }
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email Google OAuth 이메일
     * @return User 엔티티
     * @throws NoSuchElementException 사용자가 존재하지 않을 경우
     */
    fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다: email=$email") }
    }

    /**
     * 사용자 생성
     *
     * @param email Google OAuth 이메일
     * @param name 사용자 이름
     * @param profileImageUrl 프로필 이미지 URL (optional)
     * @return 생성된 User 엔티티 (id 포함)
     * @throws IllegalArgumentException 이미 존재하는 이메일일 경우
     */
    @Transactional
    fun createUser(
        email: String,
        name: String,
        profileImageUrl: String? = null
    ): User {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다: $email")
        }

        val user = User(
            email = email,
            name = name,
            profileImageUrl = profileImageUrl
        )

        return userRepository.save(user)
    }

    /**
     * 사용자 생성 또는 조회
     *
     * 이메일로 사용자 조회 → 없으면 새로 생성
     * OAuth2 로그인 시 주로 사용
     *
     * @param email Google OAuth 이메일
     * @param name 사용자 이름
     * @param profileImageUrl 프로필 이미지 URL (optional)
     * @return User 엔티티 (기존 또는 새로 생성)
     */
    @Transactional
    fun findOrCreateUser(
        email: String,
        name: String,
        profileImageUrl: String? = null
    ): User {
        return userRepository.findByEmail(email).orElseGet {
            createUser(email, name, profileImageUrl)
        }
    }

    /**
     * 이메일 존재 여부 확인
     *
     * @param email Google OAuth 이메일
     * @return 존재 여부
     */
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    /**
     * 전체 사용자 수 조회
     *
     * @return 사용자 수
     */
    fun getUserCount(): Long {
        return userRepository.count()
    }
}
