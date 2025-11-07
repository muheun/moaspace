package me.muheun.moaspace.performance

import jakarta.persistence.EntityManager
import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.dto.PostSearchRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorConfigRepository
import me.muheun.moaspace.service.PostService
import me.muheun.moaspace.service.VectorIndexingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.system.measureTimeMillis

/**
 * Vector Search 성능 테스트
 *
 * 배치 인덱싱을 활용하여 대용량 데이터 생성 성능 최적화
 */
@SpringBootTest
@Transactional
class VectorSearchPerformanceTest @Autowired constructor(
    private val postService: PostService,
    private val postRepository: PostRepository,
    private val vectorIndexingService: VectorIndexingService,
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorConfigRepository: VectorConfigRepository,
    private val userRepository: UserRepository,
    private val entityManager: EntityManager,
    private val cacheManager: org.springframework.cache.CacheManager
) {

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }

        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE posts RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE vector_chunks RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()

        testUser = userRepository.save(
            User(
                email = "perf@example.com",
                name = "Performance User"
            )
        )

        // VectorConfig 초기화 (Post 엔티티용)
        vectorConfigRepository.saveAll(
            listOf(
                VectorConfig(
                    entityType = "Post",
                    fieldName = "title",
                    enabled = true
                ),
                VectorConfig(
                    entityType = "Post",
                    fieldName = "content",
                    enabled = true
                )
            )
        )
        entityManager.flush()
    }

    @Test
    @DisplayName("1,000개 Post (약 2,000개 벡터) 검색 시 95%의 요청이 1초 이내에 응답해야 한다")
    fun testResponseTimeUnderOneSecondFor95Percent() {
        println("=== 1,000개 Post 배치 생성 시작 ===")

        val batchInsertTime = measureTimeMillis {
            // 1. Post 엔티티 배치 생성 (JPA saveAll)
            val contentText = "성능 테스트 내용입니다. " +
                    "Kotlin은 JetBrains에서 개발한 프로그래밍 언어로, " +
                    "JVM, Android, JavaScript, Native 플랫폼을 지원합니다. " +
                    "Spring Boot와의 뛰어난 호환성으로 서버 개발에서 인기가 높습니다. " +
                    "Type-safe builder, Extension function, Coroutine 등 현대적인 기능을 제공합니다. " +
                    "이 게시글은 벡터 검색 성능 테스트를 위한 샘플 데이터입니다."

            val posts = (0 until 1000).map { index ->
                Post(
                    title = "성능 테스트 게시글 #$index",
                    contentMarkdown = contentText,
                    contentHtml = "<p>$contentText</p>",
                    contentText = contentText,
                    hashtags = arrayOf("kotlin", "spring", "performance", "test-$index"),
                    author = testUser
                )
            }

            postRepository.saveAll(posts)
            postRepository.flush()
            println("${posts.size}개 Post 엔티티 생성 완료")

            // 2. VectorChunk 배치 인덱싱 (JDBC batch INSERT)
            val recordsFields = posts.map { post ->
                post.id.toString() to mapOf(
                    "title" to post.title,
                    "content" to post.contentText
                )
            }

            val chunkCount = vectorIndexingService.indexEntitiesBatch(
                entityType = "Post",
                recordsFields = recordsFields
            )

            println("${chunkCount}개 벡터 청크 생성 완료")
        }

        println("=== 배치 INSERT 소요 시간: ${batchInsertTime}ms ===")

        val vectorCount = vectorChunkRepository.count()
        println("=== 생성된 벡터 청크 개수: $vectorCount ===")
        assertThat(vectorCount).isGreaterThan(1500)
            .withFailMessage("1,500개 이상의 벡터가 생성되어야 합니다: $vectorCount")

        val searchRequest = PostSearchRequest(
            query = "Kotlin Spring Boot 성능 최적화",
            limit = 20
        )

        val responseTimes = mutableListOf<Long>()
        println("=== 100회 검색 성능 측정 시작 ===")

        repeat(100) { iteration ->
            val timeMs = measureTimeMillis {
                postService.searchPosts(searchRequest)
            }
            responseTimes.add(timeMs)

            if ((iteration + 1) % 20 == 0) {
                println("${iteration + 1}회 검색 완료 (최근 응답 시간: ${timeMs}ms)")
            }
        }

        responseTimes.sort()
        val percentile95Index = (responseTimes.size * 0.95).toInt()
        val percentile95ResponseTime = responseTimes[percentile95Index]
        val avgResponseTime = responseTimes.average()
        val maxResponseTime = responseTimes.maxOrNull() ?: 0L

        println("""
            === 성능 테스트 결과 ===
            총 벡터 개수: $vectorCount
            95번째 백분위수 응답 시간: ${percentile95ResponseTime}ms
            평균 응답 시간: ${"%.2f".format(avgResponseTime)}ms
            최대 응답 시간: ${maxResponseTime}ms
            최소 응답 시간: ${responseTimes.minOrNull()}ms
        """.trimIndent())

        assertThat(percentile95ResponseTime).isLessThan(1000)
            .withFailMessage(
                "95%의 요청이 1초 이내에 응답해야 하나 초과했습니다: ${percentile95ResponseTime}ms " +
                        "(평균: ${"%.2f".format(avgResponseTime)}ms)"
            )
    }

    @Test
    @DisplayName("소규모 데이터셋(100개 Post)에서 검색 응답 시간 검증")
    fun testSmallDatasetSearchPerformance() {
        println("=== 100개 Post 배치 생성 시작 ===")

        val posts = (0 until 100).map { index ->
            Post(
                title = "소규모 테스트 게시글 #$index",
                contentMarkdown = "소규모 테스트 내용 #$index",
                contentHtml = "<p>소규모 테스트 내용 #$index</p>",
                contentText = "소규모 테스트 내용 #$index",
                hashtags = arrayOf("small", "test"),
                author = testUser
            )
        }

        postRepository.saveAll(posts)
        postRepository.flush()

        val recordsFields = posts.map { post ->
            post.id.toString() to mapOf(
                "title" to post.title,
                "content" to post.contentText
            )
        }

        vectorIndexingService.indexEntitiesBatch(
            entityType = "Post",
            recordsFields = recordsFields
        )

        val vectorCount = vectorChunkRepository.count()
        println("생성된 벡터 개수: $vectorCount")

        val searchRequest = PostSearchRequest(
            query = "소규모 테스트",
            limit = 10
        )

        val responseTimes = mutableListOf<Long>()
        repeat(10) {
            val timeMs = measureTimeMillis {
                postService.searchPosts(searchRequest)
            }
            responseTimes.add(timeMs)
        }

        val avgTime = responseTimes.average()
        println("평균 응답 시간: ${"%.2f".format(avgTime)}ms")

        assertThat(avgTime).isLessThan(500.0)
            .withFailMessage("소규모 데이터셋 검색 평균 응답 시간이 500ms를 초과했습니다: ${"%.2f".format(avgTime)}ms")
    }
}
