package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.PostEmbedding
import me.muheun.moaspace.domain.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import jakarta.persistence.EntityManager

/**
 * PostEmbeddingRepository QueryDSL 마이그레이션 테스트
 * Constitution Principle V: 실제 DB 연동 테스트
 * Constitution Principle III: 임계값 필터링 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostEmbeddingRepositoryTest {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var postEmbeddingRepository: PostEmbeddingRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 관련 테이블들 정리 (트랜잭션 내부에서 수행)
        transactionTemplate.execute {
            entityManager.createNativeQuery("TRUNCATE TABLE post_embeddings, posts, users RESTART IDENTITY CASCADE").executeUpdate()
            entityManager.flush()
        }
        entityManager.clear()
    }

    /**
     * QueryDSL findSimilarPosts() 테스트
     *
     * Given: 3개의 게시글 + 임베딩 (유사도: 0.9, 0.7, 0.5)
     * When: findSimilarPosts(queryVector, threshold=0.6, limit=10)
     * Then: threshold 이상만 반환 (0.9, 0.7), Post와 JOIN, deleted=false 필터
     */
    @Test
    @DisplayName("[QueryDSL] testFindSimilarPosts - 벡터 유사도 검색 (JOIN + 임계값 필터링)")
    fun testFindSimilarPosts() {
        // Given: 별도 트랜잭션에서 데이터 생성 및 커밋
        val post4Id = transactionTemplate.execute {
            val user = createAndSaveUser("embedding@example.com", "임베딩 사용자")

            // 게시글 1: 높은 유사도 (0.9 예상)
            val post1 = createAndSavePost(user, "Kotlin QueryDSL 가이드", "Kotlin 내용")
            createAndSaveEmbedding(post1, createSimilarVector(0.9f))

            // 게시글 2: 중간 유사도 (0.7 예상)
            val post2 = createAndSavePost(user, "Spring Boot 튜토리얼", "Spring 내용")
            createAndSaveEmbedding(post2, createSimilarVector(0.7f))

            // 게시글 3: 낮은 유사도 (0.5 예상) - threshold 미달로 제외되어야 함
            val post3 = createAndSavePost(user, "React 컴포넌트", "React 내용")
            createAndSaveEmbedding(post3, createSimilarVector(0.5f))

            // 게시글 4: 삭제된 게시글 (높은 유사도지만 제외되어야 함)
            val post4 = createAndSavePost(user, "삭제된 게시글", "삭제 내용")
            post4.deleted = true
            entityManager.persist(post4)
            createAndSaveEmbedding(post4, createSimilarVector(0.95f))

            entityManager.flush()
            post4.id // 반환
        }!!
        // 트랜잭션 커밋 완료

        entityManager.clear()

        // When
        val queryVector = createQueryVector()
        val results = postEmbeddingRepository.findSimilarPosts(
            queryVector = queryVector,
            threshold = 0.6,
            limit = 10
        )

        // Then
        assertTrue(results.size >= 2, "threshold 0.6 이상 + deleted=false 게시글 최소 2개 반환")
        assertTrue(results.size <= 3, "최대 3개 (post1, post2, post3)")

        // deleted=true 게시글은 제외되어야 함
        assertFalse(results.any { it.postId == post4Id }, "deleted=true 게시글은 결과에서 제외")

        // 모든 결과가 threshold 이상이어야 함
        results.forEach { result ->
            assertTrue(result.similarity >= 0.6, "유사도가 threshold 이상: ${result.similarity}")
        }

        // 결과가 유사도 내림차순 정렬되었는지 확인
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].similarity >= results[i + 1].similarity, "유사도 내림차순 정렬")
        }
    }

    /**
     * QueryDSL findSimilarPosts() - limit 테스트
     *
     * Given: 5개의 게시글 + 임베딩 (모두 동일한 벡터 = threshold 1.0)
     * When: findSimilarPosts(queryVector, threshold=0.5, limit=3)
     * Then: 상위 3개만 반환
     */
    @Test
    @DisplayName("[QueryDSL] testFindSimilarPostsWithLimit - limit 파라미터 작동 확인")
    fun testFindSimilarPostsWithLimit() {
        // Given: 별도 트랜잭션에서 데이터 생성 및 커밋
        transactionTemplate.execute {
            val user = createAndSaveUser("limit@example.com", "리미트 사용자")

            // 동일한 벡터를 사용하여 유사도 1.0 보장 (queryVector와 정확히 동일)
            val identicalVector = createQueryVector()

            var lastId: Long? = null
            for (i in 1..5) {
                val post = createAndSavePost(user, "게시글 $i", "내용 $i")
                createAndSaveEmbedding(post, identicalVector) // 모두 동일한 벡터
                lastId = post.id
            }
            entityManager.flush()
            lastId // 반환값이 있어야 트랜잭션이 커밋됨
        }!!
        // 트랜잭션 커밋 완료

        entityManager.clear()

        // When
        val queryVector = createQueryVector()
        val results = postEmbeddingRepository.findSimilarPosts(
            queryVector = queryVector,
            threshold = 0.5,
            limit = 3
        )

        // Then
        assertEquals(3, results.size, "limit=3으로 상위 3개만 반환")
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

    private fun createAndSaveEmbedding(post: Post, embedding: FloatArray): PostEmbedding {
        val postEmbedding = PostEmbedding(
            post = post,
            embedding = embedding,
            modelName = "multilingual-e5-base"
        )
        entityManager.persist(postEmbedding)
        entityManager.flush()
        return postEmbedding
    }

    /**
     * 테스트용 쿼리 벡터 생성 (768차원, 모든 값 0.5)
     */
    private fun createQueryVector(): FloatArray {
        return FloatArray(768) { 0.5f }
    }

    /**
     * 유사도를 조절한 테스트 벡터 생성
     * similarity: 0.0 ~ 1.0 (1.0에 가까울수록 queryVector와 유사)
     */
    private fun createSimilarVector(similarity: Float): FloatArray {
        // 코사인 유사도를 조절하기 위해 벡터 값 조정
        // queryVector=[0.5, 0.5, ...] 와의 유사도를 similarity로 맞춤
        val vector = FloatArray(768)
        for (i in 0 until 768) {
            // 유사도가 높으면 queryVector와 비슷한 값, 낮으면 다른 값
            vector[i] = 0.5f * similarity + (1.0f - similarity) * (if (i % 2 == 0) 0.2f else 0.8f)
        }
        return vector
    }
}
