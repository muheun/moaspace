package me.muheun.moaspace.service

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.domain.vector.VectorEntityType
import me.muheun.moaspace.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val vectorIndexingService: VectorIndexingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다: $email")
        }

        return saveAndVectorize(email, name, profileImageUrl)
    }

    // User 저장 + 벡터화
    private fun saveAndVectorize(
        email: String,
        name: String,
        profileImageUrl: String?
    ): User {
        val user = User(
            email = email,
            name = name,
            profileImageUrl = profileImageUrl
        )

        val savedUser = userRepository.save(user)
        logger.info("사용자 저장 완료: userId=${savedUser.id}")

        val vectorFields = vectorIndexingService.extractVectorFields(
            entity = savedUser,
            entityType = VectorEntityType.USER.typeName
        )
        vectorIndexingService.indexEntity(
            entityType = VectorEntityType.USER.typeName,
            recordKey = savedUser.id.toString(),
            fields = vectorFields
        )
        logger.info("사용자 벡터화 완료: userId=${savedUser.id}")

        return savedUser
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
            saveAndVectorize(email, name, profileImageUrl)
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
