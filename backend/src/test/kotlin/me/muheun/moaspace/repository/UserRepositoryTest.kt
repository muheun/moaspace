package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.jdbc.Sql

/**
 * UserRepository 영속성 테스트
 * Constitution Principle V: 실제 DB 연동 테스트 (@DataJpaTest + AutoConfigureTestDatabase.Replace.NONE)
 * Mock 테스트 절대 금지
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should save user with all fields`() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = "https://via.placeholder.com/150"
        )

        // When
        val savedUser = entityManager.persistAndFlush(user)

        // Then
        assertNotNull(savedUser.id)
        assertEquals("test@example.com", savedUser.email)
        assertEquals("테스트 사용자", savedUser.name)
        assertEquals("https://via.placeholder.com/150", savedUser.profileImageUrl)
        assertNotNull(savedUser.createdAt)
    }

    @Test
    fun `should find user by email`() {
        // Given
        val user = User(
            email = "find@example.com",
            name = "검색 사용자"
        )
        entityManager.persistAndFlush(user)

        // When
        val foundUser = userRepository.findByEmail("find@example.com")

        // Then
        assertTrue(foundUser.isPresent)
        assertEquals("검색 사용자", foundUser.get().name)
    }

    @Test
    fun `should return empty when user not found by email`() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertFalse(foundUser.isPresent)
    }

    @Test
    fun `should check if user exists by email`() {
        // Given
        val user = User(
            email = "exists@example.com",
            name = "존재 확인 사용자"
        )
        entityManager.persistAndFlush(user)

        // When & Then
        assertTrue(userRepository.existsByEmail("exists@example.com"))
        assertFalse(userRepository.existsByEmail("notexists@example.com"))
    }

    @Test
    fun `should enforce unique email constraint`() {
        // Given
        val user1 = User(
            email = "unique@example.com",
            name = "사용자1"
        )
        entityManager.persistAndFlush(user1)

        // When & Then
        val user2 = User(
            email = "unique@example.com", // 동일한 이메일
            name = "사용자2"
        )

        assertThrows(Exception::class.java) {
            entityManager.persistAndFlush(user2)
        }
    }

    @Test
    fun `should save user with null profileImageUrl`() {
        // Given
        val user = User(
            email = "noimage@example.com",
            name = "이미지 없는 사용자",
            profileImageUrl = null
        )

        // When
        val savedUser = entityManager.persistAndFlush(user)

        // Then
        assertNotNull(savedUser.id)
        assertNull(savedUser.profileImageUrl)
    }
}
