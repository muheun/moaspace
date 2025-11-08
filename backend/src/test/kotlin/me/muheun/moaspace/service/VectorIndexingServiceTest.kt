package me.muheun.moaspace.service

import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.domain.vector.VectorEntityType
import me.muheun.moaspace.helper.VectorTestHelper
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
    private val vectorTestHelper: VectorTestHelper,
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

        // VectorConfig 초기 데이터 생성 (namespace는 엔티티 기본값 "moaspace" 사용)
        vectorConfigRepository.saveAll(listOf(
            VectorConfig(entityType = VectorEntityType.POST.typeName, fieldName = "title", weight = 2.0, threshold = 0.0, enabled = true),
            VectorConfig(entityType = VectorEntityType.POST.typeName, fieldName = "contentText", weight = 1.0, threshold = 0.0, enabled = true)
        ))
        entityManager.flush()
        entityManager.clear()
    }


    @Test
    @DisplayName("단일 필드 벡터화 성공")
    fun testIndexEntitySingleField() {
        // given: @BeforeEach에서 VectorConfig 이미 생성됨 (namespace="moaspace")

        // when: title 필드만 벡터화
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf("title" to "테스트 제목")
        )

        // then
        assertThat(chunkCount).isEqualTo(1)

        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(1)
        assertThat(chunks[0].namespace).isEqualTo(vectorTestHelper.defaultNamespace)
        assertThat(chunks[0].entity).isEqualTo(VectorEntityType.POST.typeName)
        assertThat(chunks[0].recordKey).isEqualTo("1")
        assertThat(chunks[0].fieldName).isEqualTo("title")
        assertThat(chunks[0].chunkText).isEqualTo("테스트 제목")
    }


    @Test
    @DisplayName("여러 필드 동시 벡터화")
    fun testIndexEntityMultipleFields() {
        // given: @BeforeEach에서 VectorConfig 이미 생성됨 (title + contentText)

        // when: VectorConfig에 설정된 모든 필드 벡터화
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "게시글 제목",
                "contentText" to "게시글 본문 내용"
            )
        )

        // then: VectorConfig 개수만큼 생성
        val expectedFieldCount = vectorConfigRepository
            .findByNamespaceAndEntityTypeAndEnabled(vectorTestHelper.defaultNamespace, VectorEntityType.POST.typeName, true)
            .size
        assertThat(chunkCount).isEqualTo(expectedFieldCount)

        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(expectedFieldCount)

        val expectedFieldNames = vectorConfigRepository
            .findByNamespaceAndEntityTypeAndEnabled(vectorTestHelper.defaultNamespace, VectorEntityType.POST.typeName, true)
            .map { it.fieldName }
        assertThat(chunks.map { it.fieldName }).containsExactlyInAnyOrderElementsOf(expectedFieldNames)
    }


    @Test
    @DisplayName("enabled=false 필드는 벡터화 건너뛰기")
    fun testIndexEntitySkipDisabledConfig() {
        // given: 기존 설정 삭제 후 enabled=false로 재생성
        vectorConfigRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
        vectorConfigRepository.saveAll(listOf(
            VectorConfig(entityType = VectorEntityType.POST.typeName, fieldName = "title", weight = 2.0, enabled = true),
            VectorConfig(entityType = VectorEntityType.POST.typeName, fieldName = "content", weight = 1.0, enabled = false)
        ))
        entityManager.flush()
        entityManager.clear()

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "게시글 제목",
                "contentText" to "게시글 본문"
            )
        )

        // then
        assertThat(chunkCount).isEqualTo(1) // title만 벡터화
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks.map { it.fieldName }).containsOnly("title")
    }

    @Test
    @DisplayName("메모리 프로파일링 - 10,000자 텍스트 처리 시 메모리 사용량 검증")
    fun testMemoryUsageForLargeContent() {
        val runtime = Runtime.getRuntime()

        // GC 실행 및 대기
        System.gc()
        Thread.sleep(100)

        // 초기 메모리 상태
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        val memoryBeforeMB = memoryBefore / 1024 / 1024

        // 10,000자 텍스트 처리
        val largeContent = "테스트 ".repeat(2000) // 10,000자 (한글 포함)

        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "memory-test",
            fields = mapOf("contentText" to largeContent)
        )

        // 최종 메모리 상태 (GC 후)
        System.gc()
        Thread.sleep(100)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryAfterMB = memoryAfter / 1024 / 1024

        val memoryIncreaseMB = memoryAfterMB - memoryBeforeMB

        println("""
            === 메모리 프로파일링 결과 ===
            초기 메모리: ${memoryBeforeMB}MB
            최종 메모리: ${memoryAfterMB}MB
            메모리 증가량: ${memoryIncreaseMB}MB
            생성된 청크 수: $chunkCount
        """.trimIndent())

        // 메모리 증가량이 150MB 이하인지 검증
        assertThat(memoryIncreaseMB).isLessThan(150)
            .withFailMessage("메모리 증가량이 너무 큽니다: ${memoryIncreaseMB}MB (목표: <150MB)")

        // 청크 수 검증 (약 18-25개, 텍스트 내용에 따라 변동)
        assertThat(chunkCount).isBetween(15, 30)
            .withFailMessage("청크 수가 예상 범위를 벗어났습니다: ${chunkCount}개 (목표: 15-30개)")
    }


    @Test
    @DisplayName("Edge Case - 10,000자 content 청킹 검증 (SC-002)")
    fun testEdgeCaseLargeContentChunking() {
        // given: @BeforeEach에서 VectorConfig 이미 생성됨

        // 10,000자 텍스트 생성
        val largeContent = "A".repeat(10000)

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "large-content",
            fields = mapOf("contentText" to largeContent)
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
        // given: @BeforeEach에서 VectorConfig 이미 생성됨

        // when: 모든 필드가 blank
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "blank-fields",
            fields = mapOf(
                "title" to "",
                "contentText" to "   "
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
        // given: @BeforeEach에서 VectorConfig 이미 생성됨

        // when
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "partial-blank",
            fields = mapOf(
                "title" to "정상 제목",
                "contentText" to "" // blank 필드
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
        // given: @BeforeEach에서 VectorConfig 이미 생성됨 + 초기 인덱싱

        vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "기존 제목",
                "contentText" to "기존 본문"
            )
        )

        // when: 재인덱싱
        val reindexCount = vectorIndexingService.reindexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "새 제목",
                "contentText" to "새 본문"
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
        // given: @BeforeEach에서 VectorConfig 이미 생성됨

        vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "제목",
                "contentText" to "본문"
            )
        )

        // when
        val deletedCount = vectorIndexingService.deleteEntityIndex(
            entityType = VectorEntityType.POST.typeName,
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
        // given: @BeforeEach에서 VectorConfig 이미 생성됨 + 초기 인덱싱

        vectorIndexingService.indexEntity(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fields = mapOf(
                "title" to "기존 제목",
                "contentText" to "기존 본문"
            )
        )

        // when: title 필드만 재인덱싱
        val reindexCount = vectorIndexingService.reindexField(
            entityType = VectorEntityType.POST.typeName,
            recordKey = "1",
            fieldName = "title",
            fieldValue = "수정된 제목"
        )

        // then
        assertThat(reindexCount).isEqualTo(1)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(2) // title + contentText

        val titleChunk = chunks.find { it.fieldName == "title" }
        val contentChunk = chunks.find { it.fieldName == "contentText" }
        assertThat(titleChunk?.chunkText).isEqualTo("수정된 제목")
        assertThat(contentChunk?.chunkText).isEqualTo("기존 본문") // 유지됨
    }
}
