package com.example.vectorai.domain.post

import com.example.vectorai.domain.user.User
import me.muheun.moaspace.VectorBoardApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql

/**
 * PostRepository 영속성 테스트
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필수 (Mock 금지)
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 */
@DataJpaTest(excludeAutoConfiguration = [
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class
])
@ContextConfiguration(classes = [VectorBoardApplication::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var postRepository: PostRepository

    @Test
    fun `should save post with HTML content and plain content`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )
        val post = Post(
            title = "테스트 게시글",
            content = "<p>HTML 내용</p>",
            plainContent = "HTML 내용", // Constitution Principle VIII
            author = user,
            hashtags = arrayOf("테스트", "게시판")
        )

        // When
        val savedPost = entityManager.persist(post)
        entityManager.flush()

        // Then
        assertNotNull(savedPost.id)
        assertEquals("테스트 게시글", savedPost.title)
        assertEquals("<p>HTML 내용</p>", savedPost.content)
        assertEquals("HTML 내용", savedPost.plainContent)
        assertArrayEquals(arrayOf("테스트", "게시판"), savedPost.hashtags)
        assertFalse(savedPost.deleted)
        assertNotNull(savedPost.createdAt)
        assertNotNull(savedPost.updatedAt)
    }

    @Test
    fun `should find non-deleted posts ordered by created date`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )

        val post1 = entityManager.persist(
            Post(
                title = "게시글 1",
                content = "<p>내용 1</p>",
                plainContent = "내용 1",
                author = user
            )
        )

        val post2 = entityManager.persist(
            Post(
                title = "게시글 2",
                content = "<p>내용 2</p>",
                plainContent = "내용 2",
                author = user,
                deleted = true // 삭제된 게시글
            )
        )

        entityManager.flush()

        // When
        val pageable = PageRequest.of(0, 10)
        val posts = postRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)

        // Then
        assertEquals(1, posts.totalElements)
        assertEquals("게시글 1", posts.content[0].title)
    }

    @Test
    fun `should find posts by author ID and not deleted`() {
        // Given
        val user1 = entityManager.persist(
            User(email = "author1@example.com", name = "작성자1")
        )
        val user2 = entityManager.persist(
            User(email = "author2@example.com", name = "작성자2")
        )

        entityManager.persist(
            Post(
                title = "User1의 게시글",
                content = "<p>내용</p>",
                plainContent = "내용",
                author = user1
            )
        )

        entityManager.persist(
            Post(
                title = "User2의 게시글",
                content = "<p>내용</p>",
                plainContent = "내용",
                author = user2
            )
        )

        entityManager.flush()

        // When
        val pageable = PageRequest.of(0, 10)
        val posts = postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
            user1.id!!,
            pageable
        )

        // Then
        assertEquals(1, posts.totalElements)
        assertEquals("User1의 게시글", posts.content[0].title)
    }

    @Test
    fun `should find posts by hashtag`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )

        entityManager.persist(
            Post(
                title = "AI 게시글",
                content = "<p>AI 내용</p>",
                plainContent = "AI 내용",
                author = user,
                hashtags = arrayOf("AI", "머신러닝")
            )
        )

        entityManager.persist(
            Post(
                title = "Java 게시글",
                content = "<p>Java 내용</p>",
                plainContent = "Java 내용",
                author = user,
                hashtags = arrayOf("Java", "Spring")
            )
        )

        entityManager.flush()

        // When
        val pageable = PageRequest.of(0, 10)
        val posts = postRepository.findByHashtagAndDeletedFalse("AI", pageable)

        // Then
        assertEquals(1, posts.totalElements)
        assertEquals("AI 게시글", posts.content[0].title)
    }

    @Test
    fun `should find post by ID and not deleted`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )
        val post = entityManager.persist(
            Post(
                title = "활성 게시글",
                content = "<p>내용</p>",
                plainContent = "내용",
                author = user
            )
        )
        entityManager.flush()

        // When
        val foundPost = postRepository.findByIdAndDeletedFalse(post.id!!)

        // Then
        assertNotNull(foundPost)
        assertEquals("활성 게시글", foundPost?.title)
    }

    @Test
    fun `should not find deleted post by ID`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )
        val post = entityManager.persist(
            Post(
                title = "삭제된 게시글",
                content = "<p>내용</p>",
                plainContent = "내용",
                author = user,
                deleted = true
            )
        )
        entityManager.flush()

        // When
        val foundPost = postRepository.findByIdAndDeletedFalse(post.id!!)

        // Then
        assertNull(foundPost)
    }

    @Test
    fun `should soft delete post`() {
        // Given
        val user = entityManager.persist(
            User(email = "author@example.com", name = "작성자")
        )
        val post = entityManager.persist(
            Post(
                title = "게시글",
                content = "<p>내용</p>",
                plainContent = "내용",
                author = user
            )
        )
        entityManager.flush()

        // When
        post.deleted = true
        entityManager.persist(post)
        entityManager.flush()

        // Then
        val foundPost = postRepository.findById(post.id!!)
        assertTrue(foundPost.isPresent)
        assertTrue(foundPost.get().deleted)
    }
}
