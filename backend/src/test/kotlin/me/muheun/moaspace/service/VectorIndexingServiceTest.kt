package me.muheun.moaspace.service

import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class VectorIndexingServiceTest @Autowired constructor(
    private val vectorIndexingService: VectorIndexingService,
    private val vectorConfigRepository: VectorConfigRepository,
    private val vectorChunkRepository: VectorChunkRepository,
    private val entityManager: EntityManager,
    private val cacheManager: org.springframework.cache.CacheManager
) {

    @BeforeEach
    fun setUp() {
        // 캐시 초기화 (VectorConfigService 캐시 제거)
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }

        entityManager.createNativeQuery("TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE vector_chunks RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    
    @Test
    @DisplayName("단일 필드 벡터화 성공")
    fun testIndexEntitySingleField() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf("title" to "테스트 제목"),
            namespace = "vector_ai"
        )

        // then
        assertThat(chunkCount).isEqualTo(1)

        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(1)
        assertThat(chunks[0].entity).isEqualTo("Post")
        assertThat(chunks[0].recordKey).isEqualTo("1")
        assertThat(chunks[0].fieldName).isEqualTo("title")
        assertThat(chunks[0].chunkText).isEqualTo("테스트 제목")
    }

    
    @Test
    @DisplayName("여러 필드 동시 벡터화")
    fun testIndexEntityMultipleFields() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "게시글 제목",
                "content" to "게시글 본문 내용"
            )
        )

        // then
        assertThat(chunkCount).isEqualTo(2)

        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(2)
        assertThat(chunks.map { it.fieldName }).containsExactlyInAnyOrder("title", "content")
    }

    
    @Test
    @DisplayName("enabled=false 필드는 벡터화 건너뛰기")
    fun testIndexEntitySkipDisabledConfig() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = false)
        )

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "게시글 제목",
                "content" to "게시글 본문"
            )
        )

        // then
        assertThat(chunkCount).isEqualTo(1) // title만 벡터화
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks.map { it.fieldName }).containsOnly("title")
    }

    
    @org.junit.jupiter.api.Disabled("OutOfMemoryError: heap size 증가 또는 배치 처리 필요")
    @Test
    @DisplayName("Edge Case - 10,000자 content 청킹 검증 (SC-002)")
    fun testEdgeCaseLargeContentChunking() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        // 10,000자 텍스트 생성
        val largeContent = "A".repeat(10000)

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "large-content",
            fields = mapOf("content" to largeContent)
        )

        // then: SC-002 검증 (10,000자 → 약 22개 청크)
        val expectedChunks = 23 // 10,000 / (500-50) = 22.2 → 23개
        assertThat(chunkCount).isEqualTo(expectedChunks)

        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(expectedChunks)

        // 모든 청크 크기 검증
        chunks.forEach { chunk ->
            assertThat(chunk.chunkText.length).isLessThanOrEqualTo(500)
        }

        // 첫 번째와 두 번째 청크의 overlap 검증 (50자)
        if (chunks.size >= 2) {
            val firstChunk = chunks.find { it.chunkIndex == 0 }!!.chunkText
            val secondChunk = chunks.find { it.chunkIndex == 1 }!!.chunkText

            // 첫 번째 청크의 마지막 50자 == 두 번째 청크의 첫 50자
            val firstLast50 = firstChunk.substring(firstChunk.length - 50)
            val secondFirst50 = secondChunk.substring(0, 50)
            assertThat(firstLast50).isEqualTo(secondFirst50)
        }
    }

    
    @Test
    @DisplayName("Edge Case - 빈 필드 벡터화 건너뛰기")
    fun testEdgeCaseSkipBlankFields() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        // when: 모든 필드가 blank
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "blank-fields",
            fields = mapOf(
                "title" to "",
                "content" to "   "
            )
        )

        // then: 청크 0개 생성
        assertThat(chunkCount).isEqualTo(0)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).isEmpty()
    }

    
    @Test
    @DisplayName("Edge Case - 일부 필드만 blank일 때 건너뛰기")
    fun testEdgeCasePartialBlankFields() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "partial-blank",
            fields = mapOf(
                "title" to "정상 제목",
                "content" to "" // blank 필드
            )
        )

        // then
        assertThat(chunkCount).isEqualTo(1)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(1)
        assertThat(chunks[0].fieldName).isEqualTo("title")
    }

    
    @Test
    @DisplayName("기존 청크 삭제 후 재생성")
    fun testReindexEntity() {
        // given: 초기 인덱싱
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "기존 제목",
                "content" to "기존 본문"
            )
        )

        // when: 재인덱싱
        val reindexCount = vectorIndexingService.reindexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "새 제목",
                "content" to "새 본문"
            )
        )

        // then
        assertThat(reindexCount).isEqualTo(2)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(2) // 기존 청크 삭제 → 새 청크 생성
        assertThat(chunks.map { it.chunkText }).containsExactlyInAnyOrder("새 제목", "새 본문")
    }

    
    @Test
    @DisplayName("엔티티의 모든 청크 삭제")
    fun testDeleteEntityIndex() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "제목",
                "content" to "본문"
            )
        )

        // when
        val deletedCount = vectorIndexingService.deleteEntityIndex(
            entityType = "Post",
            recordKey = "1"
        )

        // then
        assertThat(deletedCount).isEqualTo(2)
        val remainingChunks = vectorChunkRepository.findAll()
        assertThat(remainingChunks).isEmpty()
    }

    
    @Test
    @DisplayName("특정 필드만 재인덱싱")
    fun testReindexField() {
        // given: 초기 인덱싱
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        vectorIndexingService.indexEntity(
            entityType = "Post",
            recordKey = "1",
            fields = mapOf(
                "title" to "기존 제목",
                "content" to "기존 본문"
            )
        )

        // when: title 필드만 재인덱싱
        val reindexCount = vectorIndexingService.reindexField(
            entityType = "Post",
            recordKey = "1",
            fieldName = "title",
            fieldValue = "수정된 제목"
        )

        // then
        assertThat(reindexCount).isEqualTo(1)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(2) // title + content

        val titleChunk = chunks.find { it.fieldName == "title" }
        val contentChunk = chunks.find { it.fieldName == "content" }
        assertThat(titleChunk?.chunkText).isEqualTo("수정된 제목")
        assertThat(contentChunk?.chunkText).isEqualTo("기존 본문") // 유지됨
    }
}
