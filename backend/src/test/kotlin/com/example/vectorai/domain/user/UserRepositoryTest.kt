package com.example.vectorai.domain.user

import me.muheun.moaspace.VectorBoardApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql

/**
 * UserRepository 영속성 테스트
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필수 (Mock 금지)
 */
@DataJpaTest(excludeAutoConfiguration = [
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class
])
@ContextConfiguration(classes = [VectorBoardApplication::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `should save user and generate ID`() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = "https://via.placeholder.com/150"
        )

        // When
        val savedUser = entityManager.persist(user)
        entityManager.flush()

        // Then
        assertNotNull(savedUser.id)
        assertEquals("test@example.com", savedUser.email)
        assertEquals("테스트 사용자", savedUser.name)
        assertNotNull(savedUser.createdAt)
    }

    @Test
    fun `should find user by email`() {
        // Given
        val user = User(
            email = "unique@example.com",
            name = "유니크 사용자"
        )
        entityManager.persist(user)
        entityManager.flush()

        // When
        val foundUser = userRepository.findByEmail("unique@example.com")

        // Then
        assertTrue(foundUser.isPresent)
        assertEquals("유니크 사용자", foundUser.get().name)
    }

    @Test
    fun `should return empty optional when user not found by email`() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertTrue(foundUser.isEmpty)
    }

    @Test
    fun `should check email existence`() {
        // Given
        val user = User(
            email = "exists@example.com",
            name = "존재하는 사용자"
        )
        entityManager.persist(user)
        entityManager.flush()

        // When & Then
        assertTrue(userRepository.existsByEmail("exists@example.com"))
        assertFalse(userRepository.existsByEmail("notexists@example.com"))
    }

    @Test
    fun `should enforce unique email constraint`() {
        // Given
        val user1 = User(
            email = "duplicate@example.com",
            name = "사용자 1"
        )
        entityManager.persist(user1)
        entityManager.flush()

        // When & Then
        val user2 = User(
            email = "duplicate@example.com",
            name = "사용자 2"
        )

        assertThrows(Exception::class.java) {
            entityManager.persist(user2)
            entityManager.flush()
        }
    }
}
