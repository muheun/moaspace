package me.muheun.moaspace.service

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.dto.PostVectorSearchRequest
import me.muheun.moaspace.dto.VectorSearchResult
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import java.time.LocalDateTime

/**
 * PostVectorAdapter 통합 테스트
 *
 * 기존 Post API와 범용 벡터 시스템 간의 호환성을 검증합니다.
 * - 실제 DB 사용 (Mock 금지)
 * - @SpringBootTest로 전체 컨텍스트 로드
 * - 각 테스트 전 DB 초기화 (test-cleanup.sql)
 */
@SpringBootTest
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class PostVectorAdapterTest @Autowired constructor(
    private val postVectorAdapter: PostVectorAdapter,
    private val postRepository: PostRepository,
    private val vectorChunkRepository: VectorChunkRepository,
    private val universalVectorIndexingService: UniversalVectorIndexingService
) {

    @Test
    @DisplayName("indexPost - Post를 벡터 인덱스에 추가한다")
    fun testIndexPost() {
        // Given: 저장된 Post
        val post = Post(
            title = "Kotlin Coroutines Guide",
            content = "Kotlin의 코루틴은 비동기 프로그래밍을 위한 강력한 도구입니다. 코루틴은 경량 스레드로 동작하며 효율적입니다.",
            plainContent = "",
            author = "John Doe"
        )
        val savedPost = postRepository.save(post)

        // When: Post를 벡터 인덱스에 추가
        val future = postVectorAdapter.indexPost(savedPost)
        future.join() // 비동기 완료 대기

        // 백그라운드 작업 완료 대기 (최대 3초)
        Thread.sleep(3000)

        // Then: 벡터 청크가 생성되었는지 확인
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = savedPost.id.toString()
        )

        assertTrue(chunks.isNotEmpty(), "벡터 청크가 생성되어야 함")

        // title과 content 필드가 모두 벡터화되었는지 확인
        val titleChunks = chunks.filter { chunk -> chunk.fieldName == "title" }
        val contentChunks = chunks.filter { chunk -> chunk.fieldName == "content" }

        assertTrue(titleChunks.isNotEmpty(), "title 필드가 벡터화되어야 함")
        assertTrue(contentChunks.isNotEmpty(), "content 필드가 벡터화되어야 함")
    }

    @Test
    @DisplayName("reindexPost - Post를 재인덱싱한다 (기존 청크 삭제 후 새로 생성)")
    fun testReindexPost() {
        // Given: 저장된 Post
        val post = Post(
            title = "Spring Boot Guide",
            content = "Spring Boot는 스프링 기반 애플리케이션을 쉽게 만들 수 있게 해줍니다.",
            plainContent = "",
            author = "Jane Doe"
        )
        val savedPost = postRepository.save(post)

        // 초기 인덱싱
        postVectorAdapter.indexPost(savedPost).join()
        Thread.sleep(3000) // 백그라운드 작업 대기

        val initialChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = savedPost.id.toString()
        )
        val initialChunkCount = initialChunks.size

        // When: Post 수정 후 재인덱싱
        savedPost.title = "Spring Boot Complete Guide"
        savedPost.content = "Spring Boot는 스프링 기반 애플리케이션을 쉽게 만들 수 있게 해줍니다. 자동 설정과 임베디드 서버를 제공합니다. 마이크로서비스에 적합합니다."
        postRepository.save(savedPost)

        postVectorAdapter.reindexPost(savedPost).join()
        Thread.sleep(3000) // 백그라운드 작업 대기

        // Then: 새로운 청크가 생성되고, 기존 청크는 삭제되었는지 확인
        val newChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = savedPost.id.toString()
        )

        assertTrue(newChunks.isNotEmpty(), "새로운 벡터 청크가 생성되어야 함")
        // 내용이 길어졌으므로 청크 수가 증가했을 것으로 예상
        assertTrue(newChunks.size >= initialChunkCount, "청크 수가 증가하거나 유지되어야 함")

        // 기존 청크 ID들이 남아있지 않은지 확인
        val oldChunkIds = initialChunks.map { chunk -> chunk.id }.toSet()
        val newChunkIds = newChunks.map { chunk -> chunk.id }.toSet()
        val remainingOldChunks = oldChunkIds.intersect(newChunkIds)

        assertTrue(remainingOldChunks.isEmpty(), "기존 청크가 완전히 삭제되어야 함 (고아 청크 0개)")
    }

    @Test
    @DisplayName("deletePost - Post를 벡터 인덱스에서 삭제한다")
    fun testDeletePost() {
        // Given: 저장 및 인덱싱된 Post
        val post = Post(
            title = "PostgreSQL Guide",
            content = "PostgreSQL은 강력한 오픈소스 관계형 데이터베이스입니다.",
            plainContent = "",
            author = "Alice"
        )
        val savedPost = postRepository.save(post)

        postVectorAdapter.indexPost(savedPost).join()
        Thread.sleep(3000) // 백그라운드 작업 대기

        val chunksBeforeDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = savedPost.id.toString()
        )
        assertTrue(chunksBeforeDelete.isNotEmpty(), "인덱싱 후 청크가 존재해야 함")

        // When: Post 삭제
        postVectorAdapter.deletePost(savedPost.id!!)

        // Then: 벡터 청크도 함께 삭제되었는지 확인
        val chunksAfterDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace = "vector_ai",
            entity = "posts",
            recordKey = savedPost.id.toString()
        )

        assertTrue(chunksAfterDelete.isEmpty(), "삭제 후 청크가 모두 제거되어야 함")
    }

    @Test
    @DisplayName("searchPosts - title과 content를 가중치 적용하여 검색한다")
    fun testSearchPosts() {
        // Given: 여러 Post 저장 및 인덱싱
        val post1 = Post(
            title = "PostgreSQL 완벽 가이드",
            content = "MySQL은 가벼운 데이터베이스입니다.",
            plainContent = "",
            author = "Bob"
        )
        val post2 = Post(
            title = "MySQL 튜토리얼",
            content = "PostgreSQL은 고급 기능을 제공하는 데이터베이스입니다.",
            plainContent = "",
            author = "Carol"
        )
        val post3 = Post(
            title = "MongoDB 가이드",
            content = "MongoDB는 NoSQL 데이터베이스입니다.",
            plainContent = "",
            author = "David"
        )

        val savedPost1 = postRepository.save(post1)
        val savedPost2 = postRepository.save(post2)
        val savedPost3 = postRepository.save(post3)

        postVectorAdapter.indexPost(savedPost1).join()
        postVectorAdapter.indexPost(savedPost2).join()
        postVectorAdapter.indexPost(savedPost3).join()
        Thread.sleep(5000) // 백그라운드 작업 대기 (여러 개)

        // When: "PostgreSQL"로 검색 (title 60%, content 40% 가중치)
        val request = PostVectorSearchRequest(
            query = "PostgreSQL",
            limit = 10
        )
        val results = postVectorAdapter.searchPosts(request)

        // Then: 검색 결과 검증
        assertTrue(results.isNotEmpty(), "검색 결과가 존재해야 함")

        // Post1 (title에 PostgreSQL)과 Post2 (content에 PostgreSQL)가 검색되어야 함
        val resultPostIds = results.map { it.post.id }.toSet()
        assertTrue(resultPostIds.contains(savedPost1.id), "Post1이 검색되어야 함 (title에 PostgreSQL)")
        assertTrue(resultPostIds.contains(savedPost2.id), "Post2가 검색되어야 함 (content에 PostgreSQL)")

        // Post1이 Post2보다 높은 점수를 가져야 함 (title 가중치가 더 높음)
        val post1Result = results.find { it.post.id == savedPost1.id }
        val post2Result = results.find { it.post.id == savedPost2.id }

        if (post1Result != null && post2Result != null) {
            assertTrue(
                post1Result.similarityScore!! > post2Result.similarityScore!!,
                "title 매칭(Post1)이 content 매칭(Post2)보다 높은 점수를 가져야 함"
            )
        }
    }

    @Test
    @DisplayName("searchByField - 특정 필드만 검색한다")
    fun testSearchByField() {
        // Given: 저장 및 인덱싱된 Post
        val post = Post(
            title = "Kotlin Coroutines",
            content = "Java의 CompletableFuture와 비교하면 코루틴이 더 간결합니다.",
            plainContent = "",
            author = "Eve"
        )
        val savedPost = postRepository.save(post)
        postVectorAdapter.indexPost(savedPost).join()
        Thread.sleep(3000) // 백그라운드 작업 대기

        // When: title 필드만 검색
        val titleResults = postVectorAdapter.searchByField(
            query = "Kotlin",
            fieldName = "title",
            limit = 10
        )

        // Then: title 필드에서 검색되어야 함
        assertTrue(titleResults.isNotEmpty(), "title 필드 검색 결과가 존재해야 함")
        assertEquals(savedPost.id, titleResults.first().post.id, "검색된 Post가 일치해야 함")

        // When: content 필드만 검색
        val contentResults = postVectorAdapter.searchByField(
            query = "Java",
            fieldName = "content",
            limit = 10
        )

        // Then: content 필드에서 검색되어야 함
        assertTrue(contentResults.isNotEmpty(), "content 필드 검색 결과가 존재해야 함")
        assertEquals(savedPost.id, contentResults.first().post.id, "검색된 Post가 일치해야 함")
    }

    @Test
    @DisplayName("기존 API 호환성 - PostVectorSearchRequest를 그대로 사용할 수 있다")
    fun testBackwardCompatibility() {
        // Given: 기존 방식대로 Post 생성
        val post = Post(
            title = "Spring Data JPA Guide",
            content = "Spring Data JPA는 JPA를 쉽게 사용할 수 있게 해줍니다.",
            plainContent = "",
            author = "Frank"
        )
        val savedPost = postRepository.save(post)
        postVectorAdapter.indexPost(savedPost).join()
        Thread.sleep(3000)

        // When: 기존 PostVectorSearchRequest 사용
        val request = PostVectorSearchRequest(
            query = "JPA",
            limit = 10
        )
        val results = postVectorAdapter.searchPosts(request)

        // Then: 기존 방식과 동일하게 동작해야 함
        assertTrue(results.isNotEmpty(), "검색 결과가 존재해야 함")
        assertEquals(savedPost.id, results.first().post.id, "검색된 Post가 일치해야 함")
        assertNotNull(results.first().similarityScore, "유사도 점수가 포함되어야 함")
        assertNotNull(results.first().matchedChunkText, "매칭된 청크 텍스트가 포함되어야 함")
        assertNotNull(results.first().chunkPosition, "청크 위치 정보가 포함되어야 함")
    }
}
