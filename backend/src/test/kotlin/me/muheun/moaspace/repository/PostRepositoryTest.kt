package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach

/**
 * PostRepository 영속성 테스트
 * Constitution Principle V: 실제 DB 연동 테스트 (@DataJpaTest + AutoConfigureTestDatabase.Replace.NONE)
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장 검증
 * Mock 테스트 절대 금지
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostRepositoryTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 관련 테이블들 정리 (CASCADE로 외래 키 처리)
        entityManager.createNativeQuery("TRUNCATE TABLE posts, users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    /**
     * Post 저장 테스트 (Constitution Principle VIII 준수)
     *
     * Given: content (HTML) + plainContent (Plain Text) 분리된 Post 엔티티
     * When: persistAndFlush() 호출
     * Then: 모든 필드 정상 저장 및 타임스탬프 자동 생성
     */
    @Test
    @DisplayName("testSavePost - content와 plainContent를 분리하여 Post를 저장한다")
    fun testSavePost() {
        // Given
        val user = createAndSaveUser("post@example.com", "게시글 작성자")
        val post = Post(
            title = "테스트 게시글",
            contentMarkdown = "HTML 콘텐츠",
            contentHtml = "<p>HTML 콘텐츠</p>",
            contentText = "HTML 콘텐츠", // Constitution Principle VIII
            author = user,
            hashtags = arrayOf("테스트", "게시판")
        )

        // When
        entityManager.persist(post)
        entityManager.flush()
        val savedPost = post

        // Then
        assertNotNull(savedPost.id)
        assertEquals("테스트 게시글", savedPost.title)
        assertEquals("HTML 콘텐츠", savedPost.contentMarkdown)
        assertEquals("<p>HTML 콘텐츠</p>", savedPost.contentHtml)
        assertEquals("HTML 콘텐츠", savedPost.contentText)
        assertEquals(2, savedPost.hashtags.size)
        assertFalse(savedPost.deleted)
        assertNotNull(savedPost.createdAt)
        assertNotNull(savedPost.updatedAt)
    }

    /**
     * 삭제되지 않은 게시글만 조회 테스트
     *
     * Given: 활성 게시글 1개 + 삭제된 게시글 1개
     * When: findByDeletedFalse() 호출
     * Then: 활성 게시글만 조회 (deleted=false)
     */
    @Test
    @DisplayName("testFindNonDeletedPosts - deleted가 false인 게시글만 조회한다")
    fun testFindNonDeletedPosts() {
        // Given
        val user = createAndSaveUser("find@example.com", "검색 사용자")
        createAndSavePost(user, "활성 게시글", "내용1")
        val deletedPost = createAndSavePost(user, "삭제된 게시글", "내용2")
        deletedPost.deleted = true
        entityManager.persist(deletedPost)
        entityManager.flush()

        // When
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        val activePosts = postRepository.findByDeletedFalse(pageable)

        // Then
        assertEquals(1, activePosts.totalElements)
        assertEquals("활성 게시글", activePosts.content[0].title)
    }

    /**
     * 작성자별 게시글 조회 테스트
     *
     * Given: 작성자1(1개), 작성자2(1개) 게시글 저장
     * When: findByAuthorAndDeletedFalse(작성자1) 호출
     * Then: 작성자1의 게시글만 조회
     */
    @Test
    @DisplayName("testFindByAuthor - 특정 작성자의 게시글만 조회한다")
    fun testFindByAuthor() {
        // Given
        val user1 = createAndSaveUser("author1@example.com", "작성자1")
        val user2 = createAndSaveUser("author2@example.com", "작성자2")
        createAndSavePost(user1, "작성자1의 게시글", "내용1")
        createAndSavePost(user2, "작성자2의 게시글", "내용2")

        // When
        val pageable = PageRequest.of(0, 10)
        val user1Posts = postRepository.findByAuthorAndDeletedFalse(user1, pageable)

        // Then
        assertEquals(1, user1Posts.totalElements)
        assertEquals("작성자1의 게시글", user1Posts.content[0].title)
    }

    /**
     * 해시태그별 게시글 조회 테스트
     *
     * Given: "Kotlin" 태그 1개, "Java" 태그 1개 게시글
     * When: findByHashtag("Kotlin") 호출
     * Then: "Kotlin" 태그 게시글만 조회
     */
    @Test
    @DisplayName("testFindByHashtag - 특정 해시태그를 가진 게시글을 조회한다")
    fun testFindByHashtag() {
        // Given
        val user = createAndSaveUser("hashtag@example.com", "해시태그 사용자")
        val post1 = Post(
            title = "Kotlin 게시글",
            contentMarkdown = "Kotlin 내용",
            contentHtml = "<p>Kotlin 내용</p>",
            contentText = "Kotlin 내용",
            author = user,
            hashtags = arrayOf("Kotlin", "Backend")
        )
        val post2 = Post(
            title = "Java 게시글",
            contentMarkdown = "Java 내용",
            contentHtml = "<p>Java 내용</p>",
            contentText = "Java 내용",
            author = user,
            hashtags = arrayOf("Java", "Backend")
        )
        entityManager.persist(post1)
        entityManager.persist(post2)
        entityManager.flush()

        // When
        val posts = postRepository.findByHashtag("Kotlin", limit = 10, offset = 0)
        val total = postRepository.countByHashtag("Kotlin")

        // Then
        assertEquals(1, total)
        assertEquals(1, posts.size)
        assertEquals("Kotlin 게시글", posts[0].title)
    }

    /**
     * 제목 검색 테스트 (대소문자 무시)
     *
     * Given: "Spring Boot" 제목 1개, "Kotlin Coroutines" 제목 1개
     * When: findByTitleContainingIgnoreCase("spring") 호출
     * Then: "Spring" 포함 게시글만 조회
     */
    @Test
    @DisplayName("testFindByTitleContaining - 제목에 특정 키워드가 포함된 게시글을 조회한다")
    fun testFindByTitleContaining() {
        // Given
        val user = createAndSaveUser("search@example.com", "검색 사용자")
        createAndSavePost(user, "Spring Boot 튜토리얼", "내용1")
        createAndSavePost(user, "Kotlin Coroutines", "내용2")

        // When
        val pageable = PageRequest.of(0, 10)
        val springPosts = postRepository.findByTitleContainingIgnoreCaseAndDeletedFalse("spring", pageable)

        // Then
        assertEquals(1, springPosts.totalElements)
        assertEquals("Spring Boot 튜토리얼", springPosts.content[0].title)
    }

    /**
     * Post 수정 및 updatedAt 타임스탬프 갱신 테스트
     *
     * Given: 기존 게시글 저장
     * When: 제목 및 내용 수정 후 persistAndFlush()
     * Then: 수정된 값 반영 + updatedAt 타임스탬프 갱신
     */
    @Test
    @DisplayName("testUpdatePost - Post 수정 시 updatedAt 타임스탬프가 갱신된다")
    fun testUpdatePost() {
        // Given
        val user = createAndSaveUser("update@example.com", "수정 사용자")
        val post = createAndSavePost(user, "원래 제목", "원래 내용")
        val originalUpdatedAt = post.updatedAt

        // When
        Thread.sleep(100) // 시간 차이를 두기 위해
        post.title = "수정된 제목"
        post.contentMarkdown = "수정된 내용"
        post.contentHtml = "<p>수정된 내용</p>"
        post.contentText = "수정된 내용"
        entityManager.persist(post)
        entityManager.flush()
        val updatedPost = post

        // Then
        assertEquals("수정된 제목", updatedPost.title)
        assertEquals("수정된 내용", updatedPost.contentMarkdown)
        assertEquals("<p>수정된 내용</p>", updatedPost.contentHtml)
        assertEquals("수정된 내용", updatedPost.contentText)
        assertTrue(updatedPost.updatedAt.isAfter(originalUpdatedAt))
    }

    /**
     * Post 소프트 삭제 테스트
     *
     * Given: 기존 게시글 저장
     * When: deleted=true 설정 후 persistAndFlush()
     * Then: DB에 남아있지만 deleted=true + 목록 조회 시 제외됨
     */
    @Test
    @DisplayName("testSoftDeletePost - Post 소프트 삭제 시 deleted 플래그가 true로 설정된다")
    fun testSoftDeletePost() {
        // Given
        val user = createAndSaveUser("delete@example.com", "삭제 사용자")
        val post = createAndSavePost(user, "삭제될 게시글", "내용")

        // When
        post.deleted = true
        entityManager.persist(post)
        entityManager.flush()

        // Then
        val deletedPost = postRepository.findById(post.id!!).orElse(null)
        assertNotNull(deletedPost)
        assertTrue(deletedPost.deleted)

        // 목록 조회 시 제외되는지 확인
        val pageable = PageRequest.of(0, 10)
        val activePosts = postRepository.findByDeletedFalse(pageable)
        assertEquals(0, activePosts.totalElements)
    }

    // ========== QueryDSL 마이그레이션 테스트 (Phase 3) ==========

    /**
     * QueryDSL search() - 제목 검색 테스트
     *
     * Given: "Spring Boot" 제목 1개, "Kotlin" 제목 1개
     * When: search(filter={title="spring"}) 호출
     * Then: "Spring" 포함 게시글만 조회
     */
    @Test
    @DisplayName("[QueryDSL] testSearchByTitle - 제목으로 동적 검색한다")
    fun testSearchByTitle() {
        // Given
        val user = createAndSaveUser("querydsl_title@example.com", "QueryDSL 테스트")
        createAndSavePost(user, "Spring Boot 마이그레이션", "Spring 내용")
        createAndSavePost(user, "Kotlin DSL 가이드", "Kotlin 내용")

        // When
        val filter = me.muheun.moaspace.query.dto.PostSearchFilter(title = "spring")
        val pageable = PageRequest.of(0, 10)
        val results = postRepository.search(filter, pageable)

        // Then
        assertEquals(1, results.totalElements)
        assertEquals("Spring Boot 마이그레이션", results.content[0].title)
    }

    /**
     * QueryDSL search() - 작성자 + 제목 복합 검색 테스트
     *
     * Given: 작성자A("Spring" 제목), 작성자B("Spring" 제목)
     * When: search(filter={author=A, title="spring"}) 호출
     * Then: 작성자A의 "Spring" 게시글만 조회
     */
    @Test
    @DisplayName("[QueryDSL] testSearchByAuthorAndTitle - 여러 조건을 결합하여 검색한다")
    fun testSearchByAuthorAndTitle() {
        // Given
        val userA = createAndSaveUser("author_a@example.com", "작성자A")
        val userB = createAndSaveUser("author_b@example.com", "작성자B")
        createAndSavePost(userA, "Spring Boot Tutorial", "내용A")
        createAndSavePost(userB, "Spring Cloud Gateway", "내용B")
        createAndSavePost(userA, "Kotlin Coroutines", "내용C")

        // When
        val filter = me.muheun.moaspace.query.dto.PostSearchFilter(
            author = "작성자A",
            title = "spring"
        )
        val pageable = PageRequest.of(0, 10)
        val results = postRepository.search(filter, pageable)

        // Then
        assertEquals(1, results.totalElements)
        assertEquals("Spring Boot Tutorial", results.content[0].title)
        assertEquals("작성자A", results.content[0].author.name)
    }

    /**
     * QueryDSL search() - 해시태그 검색 테스트 (PostgreSQL ANY 연산자)
     *
     * Given: "QueryDSL" 태그 1개, "JPA" 태그 1개
     * When: search(filter={hashtag="QueryDSL"}) 호출
     * Then: "QueryDSL" 태그 게시글만 조회
     */
    @Test
    @DisplayName("[QueryDSL] testSearchByHashtag - PostgreSQL ANY 배열 연산자로 해시태그를 검색한다")
    fun testSearchByHashtag() {
        // Given
        val user = createAndSaveUser("querydsl_hashtag@example.com", "태그 사용자")
        val post1 = Post(
            title = "QueryDSL 마이그레이션",
            contentMarkdown = "QueryDSL 내용",
            contentHtml = "<p>QueryDSL 내용</p>",
            contentText = "QueryDSL 내용",
            author = user,
            hashtags = arrayOf("QueryDSL", "Migration")
        )
        val post2 = Post(
            title = "JPA 성능 최적화",
            contentMarkdown = "JPA 내용",
            contentHtml = "<p>JPA 내용</p>",
            contentText = "JPA 내용",
            author = user,
            hashtags = arrayOf("JPA", "Performance")
        )
        entityManager.persist(post1)
        entityManager.persist(post2)
        entityManager.flush()

        // When
        val filter = me.muheun.moaspace.query.dto.PostSearchFilter(hashtag = "QueryDSL")
        val pageable = PageRequest.of(0, 10)
        val results = postRepository.search(filter, pageable)

        // Then
        assertEquals(1, results.totalElements)
        assertEquals("QueryDSL 마이그레이션", results.content[0].title)
    }

    /**
     * QueryDSL countByHashtag() - 해시태그 카운트 테스트
     *
     * Given: "Backend" 태그 2개, "Frontend" 태그 1개
     * When: countByHashtag("Backend") 호출
     * Then: 2 반환
     */
    @Test
    @DisplayName("[QueryDSL] testCountByHashtag - ANY 연산자로 올바른 카운트를 반환한다")
    fun testCountByHashtagQueryDSL() {
        // Given
        val user = createAndSaveUser("count@example.com", "카운트 사용자")
        val post1 = Post(
            title = "게시글1",
            contentMarkdown = "내용1",
            contentHtml = "<p>내용1</p>",
            contentText = "내용1",
            author = user,
            hashtags = arrayOf("Backend", "Kotlin")
        )
        val post2 = Post(
            title = "게시글2",
            contentMarkdown = "내용2",
            contentHtml = "<p>내용2</p>",
            contentText = "내용2",
            author = user,
            hashtags = arrayOf("Backend", "Java")
        )
        val post3 = Post(
            title = "게시글3",
            contentMarkdown = "내용3",
            contentHtml = "<p>내용3</p>",
            contentText = "내용3",
            author = user,
            hashtags = arrayOf("Frontend", "React")
        )
        entityManager.persist(post1)
        entityManager.persist(post2)
        entityManager.persist(post3)
        entityManager.flush()

        // When
        val backendCount = postRepository.countByHashtag("Backend")
        val frontendCount = postRepository.countByHashtag("Frontend")

        // Then
        assertEquals(2, backendCount)
        assertEquals(1, frontendCount)
    }

    // Helper Methods
    private fun createAndSaveUser(email: String, name: String): User {
        val user = User(email = email, name = name)
        entityManager.persist(user)
        entityManager.flush()
        return user
    }

    private fun createAndSavePost(user: User, title: String, textContent: String): Post {
        val post = Post(
            title = title,
            contentMarkdown = textContent,
            contentHtml = "<p>$textContent</p>",
            contentText = textContent,
            author = user
        )
        entityManager.persist(post)
        entityManager.flush()
        return post
    }
}
