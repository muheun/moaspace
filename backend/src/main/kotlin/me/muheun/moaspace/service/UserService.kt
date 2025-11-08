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

    fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다: userId=$userId") }
    }

    fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다: email=$email") }
    }

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

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun getUserCount(): Long {
        return userRepository.count()
    }
}
