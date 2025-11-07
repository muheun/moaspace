package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import jakarta.persistence.EntityManager
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 users 테이블 정리
        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("User를 모든 필드와 함께 저장한다")
    fun testSaveUser() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = "https://via.placeholder.com/150"
        )

        // When
        val savedUser = userRepository.save(user)
        entityManager.flush()

        // Then
        assertNotNull(savedUser.id)
        assertEquals("test@example.com", savedUser.email)
        assertEquals("테스트 사용자", savedUser.name)
        assertEquals("https://via.placeholder.com/150", savedUser.profileImageUrl)
        assertNotNull(savedUser.createdAt)
    }

    @Test
    @DisplayName("이메일로 User를 조회한다")
    fun testFindByEmail() {
        // Given
        val user = User(
            email = "find@example.com",
            name = "검색 사용자"
        )
        userRepository.save(user)
        entityManager.flush()

        // When
        val foundUser = userRepository.findByEmail("find@example.com")

        // Then
        assertTrue(foundUser.isPresent)
        assertEquals("검색 사용자", foundUser.get().name)
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 empty를 반환한다")
    fun testFindByEmailNotFound() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertFalse(foundUser.isPresent)
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인한다")
    fun testExistsByEmail() {
        // Given
        val user = User(
            email = "exists@example.com",
            name = "존재 확인 사용자"
        )
        userRepository.save(user)
        entityManager.flush()

        // When & Then
        assertTrue(userRepository.existsByEmail("exists@example.com"))
        assertFalse(userRepository.existsByEmail("notexists@example.com"))
    }

    @Test
    @DisplayName("중복된 이메일 저장 시 예외가 발생한다")
    fun testUniqueEmailConstraint() {
        // Given
        val user1 = User(
            email = "unique@example.com",
            name = "사용자1"
        )
        userRepository.save(user1)
        entityManager.flush()

        // When & Then
        val user2 = User(
            email = "unique@example.com", // 동일한 이메일
            name = "사용자2"
        )

        assertThrows(Exception::class.java) {
            userRepository.save(user2)
            entityManager.flush()
        }
    }

    @Test
    @DisplayName("profileImageUrl이 null인 User를 저장한다")
    fun testSaveUserWithNullProfileImage() {
        // Given
        val user = User(
            email = "noimage@example.com",
            name = "이미지 없는 사용자",
            profileImageUrl = null
        )

        // When
        val savedUser = userRepository.save(user)
        entityManager.flush()

        // Then
        assertNotNull(savedUser.id)
        assertNull(savedUser.profileImageUrl)
    }
}
