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

/**
 * UserRepository 영속성 테스트
 * Constitution Principle V: 실제 DB 연동 테스트 (@SpringBootTest + @Transactional)
 * Mock 테스트 절대 금지
 *
 * @Transactional: 각 테스트가 트랜잭션 내에서 실행되고 종료 후 롤백되어 DB 격리 보장
 */
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

    /**
     * User 저장 테스트 (모든 필드)
     *
     * Given: email, name, profileImageUrl 포함 User
     * When: persistAndFlush() 호출
     * Then: 모든 필드 정상 저장 + ID 및 createdAt 자동 생성
     */
    @Test
    @DisplayName("testSaveUser - User를 모든 필드와 함께 저장한다")
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

    /**
     * 이메일로 User 조회 테스트
     *
     * Given: 특정 이메일의 User 저장
     * When: findByEmail() 호출
     * Then: 해당 User 조회 성공
     */
    @Test
    @DisplayName("testFindByEmail - 이메일로 User를 조회한다")
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

    /**
     * 존재하지 않는 이메일 조회 테스트
     *
     * Given: 데이터 없음
     * When: 존재하지 않는 이메일로 findByEmail() 호출
     * Then: Optional.empty() 반환
     */
    @Test
    @DisplayName("testFindByEmailNotFound - 존재하지 않는 이메일 조회 시 empty를 반환한다")
    fun testFindByEmailNotFound() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertFalse(foundUser.isPresent)
    }

    /**
     * 이메일 존재 여부 확인 테스트
     *
     * Given: 특정 이메일의 User 저장
     * When: existsByEmail() 호출
     * Then: 존재하는 이메일은 true, 없는 이메일은 false 반환
     */
    @Test
    @DisplayName("testExistsByEmail - 이메일 존재 여부를 확인한다")
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

    /**
     * 이메일 UNIQUE 제약 테스트
     *
     * Given: 특정 이메일의 User 이미 존재
     * When: 동일한 이메일로 새 User 저장 시도
     * Then: Exception 발생 (UNIQUE 제약 위반)
     */
    @Test
    @DisplayName("testUniqueEmailConstraint - 중복된 이메일 저장 시 예외가 발생한다")
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

    /**
     * profileImageUrl null 저장 테스트
     *
     * Given: profileImageUrl이 null인 User
     * When: persistAndFlush() 호출
     * Then: 정상 저장 + profileImageUrl null 유지
     */
    @Test
    @DisplayName("testSaveUserWithNullProfileImage - profileImageUrl이 null인 User를 저장한다")
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
