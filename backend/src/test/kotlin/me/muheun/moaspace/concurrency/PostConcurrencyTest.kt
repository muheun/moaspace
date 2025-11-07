package me.muheun.moaspace.concurrency

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorConfigRepository
import me.muheun.moaspace.service.PostService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Post 동시성 테스트
 *
 * 100개 Post 동시 생성 시 데드락이 발생하지 않는지 검증
 *
 * @Transactional 제거: 별도 스레드는 트랜잭션을 상속하지 않으므로
 * 각 스레드가 독립적인 트랜잭션을 생성하도록 허용
 */
@SpringBootTest
class PostConcurrencyTest @Autowired constructor(
    private val postService: PostService,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorConfigRepository: VectorConfigRepository,
    private val cacheManager: org.springframework.cache.CacheManager
) {

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }

        // 모든 테이블 데이터 삭제
        vectorChunkRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
        vectorConfigRepository.deleteAll()

        // VectorConfig 초기 데이터 생성
        vectorConfigRepository.saveAll(listOf(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, threshold = 0.0, enabled = true),
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, threshold = 0.0, enabled = true)
        ))

        testUser = userRepository.save(
            User(
                email = "test@example.com",
                name = "Test User"
            )
        )
    }

    @AfterEach
    fun cleanup() {
        vectorChunkRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
        vectorConfigRepository.deleteAll()
    }

    @Test
    @DisplayName("100개 Post 동시 생성 시 데드락이 발생하지 않아야 한다")
    fun testConcurrentPostCreationWithoutDeadlock() {
        val threadCount = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        repeat(threadCount) { index ->
            executor.submit {
                try {
                    val request = CreatePostRequest(
                        title = "동시성 테스트 게시글 #$index",
                        contentHtml = "<p>동시성 테스트 내용 #$index</p>",
                        hashtags = listOf("concurrency", "test")
                    )
                    postService.createPost(request, testUser.id!!)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                    println("Error creating post #$index: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()

        println("=== 동시성 테스트 결과 ===")
        println("완료 여부: $completed")
        println("성공: ${successCount.get()}건")
        println("실패: ${errorCount.get()}건")

        assertThat(completed).isTrue
            .withFailMessage("모든 스레드가 180초 내에 완료되지 않았습니다 (성공: ${successCount.get()}, 실패: ${errorCount.get()})")

        assertThat(errorCount.get()).isEqualTo(0)
            .withFailMessage("데드락 또는 에러 발생: ${errorCount.get()}건")

        assertThat(successCount.get()).isEqualTo(threadCount)
            .withFailMessage("성공한 Post 생성 수가 예상과 다릅니다: ${successCount.get()}/$threadCount")

        val savedPostCount = postRepository.count()
        assertThat(savedPostCount).isEqualTo(threadCount.toLong())
            .withFailMessage("DB에 저장된 Post 개수가 예상과 다릅니다: $savedPostCount/$threadCount")
    }

    @Test
    @DisplayName("동일 사용자가 동시에 여러 Post를 생성할 때 벡터 인덱싱이 정상적으로 처리되어야 한다")
    fun testConcurrentVectorIndexingBySameUser() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)

        repeat(threadCount) { index ->
            executor.submit {
                try {
                    val request = CreatePostRequest(
                        title = "벡터 인덱싱 테스트 #$index",
                        contentHtml = "<p>벡터 인덱싱 테스트 내용 #$index</p>",
                        hashtags = listOf("vector", "indexing")
                    )
                    postService.createPost(request, testUser.id!!)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        assertThat(successCount.get()).isEqualTo(threadCount)
        assertThat(postRepository.count()).isEqualTo(threadCount.toLong())
    }
}
