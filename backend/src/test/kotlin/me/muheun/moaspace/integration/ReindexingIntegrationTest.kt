package me.muheun.moaspace.integration

import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.service.UniversalVectorIndexingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

/**
 * 재인덱싱 일관성 Integration Test
 *
 * Phase 6: User Story 5 - 재인덱싱 시 데이터 일관성 보장
 *
 * 검증 시나리오:
 * 1. 기본 재인덱싱 - 기존 청크 완전 삭제 후 새 청크 생성
 * 2. 삭제 시 청크 정리 - 레코드 삭제 시 모든 청크 함께 삭제
 * 3. 고아 청크 발생률 0% - 재인덱싱 후 고아 청크 없음
 */
@SpringBootTest
@DisplayName("재인덱싱 일관성 Integration Test")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ReindexingIntegrationTest {

    @Autowired
    private lateinit var universalVectorIndexingService: UniversalVectorIndexingService

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    private val testNamespace = "test_namespace"
    private val testEntity = "products"

    @BeforeEach
    fun setUp() {
        // 비동기 작업 완료 대기
        Thread.sleep(500)
    }

    @Test
    @DisplayName("시나리오 1: 재인덱싱 시 기존 청크가 완전히 삭제되고 새 청크만 생성된다")
    fun `should completely remove old chunks and create new ones on reindexing`() {
        // given - 초기 인덱싱 (짧은 텍스트로 3개 청크 생성 예상)
        val recordKey = "product-001"
        val initialRequest = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey,
            fields = mapOf(
                "name" to "스마트폰 갤럭시",
                "description" to "최신 스마트폰입니다. 고성능 프로세서 탑재."
            )
        )

        universalVectorIndexingService.indexEntity(initialRequest)
        Thread.sleep(1500) // 비동기 벡터 생성 대기

        // 초기 청크 수 확인
        val initialChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey
        )
        val initialChunkCount = initialChunks.size
        assertThat(initialChunkCount).isGreaterThan(0)
        println("🔵 [초기 인덱싱] 청크 수: $initialChunkCount")

        // when - 재인덱싱 (더 긴 텍스트로 더 많은 청크 생성 예상)
        val reindexRequest = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey,
            fields = mapOf(
                "name" to "스마트폰 갤럭시 S24 Ultra",
                "description" to "최신 플래그십 스마트폰입니다. " +
                        "고성능 Snapdragon 프로세서 탑재. " +
                        "200MP 카메라와 5000mAh 배터리 내장. " +
                        "6.8인치 Dynamic AMOLED 디스플레이. " +
                        "S펜 내장으로 노트 작성 가능. " +
                        "5G 네트워크 지원으로 초고속 인터넷."
            )
        )

        universalVectorIndexingService.reindexEntity(reindexRequest)
        Thread.sleep(2000) // 비동기 벡터 생성 대기

        // then - 재인덱싱 후 청크 확인
        val reindexedChunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey
        )
        val reindexedChunkCount = reindexedChunks.size
        println("🟢 [재인덱싱] 청크 수: $reindexedChunkCount")

        // 1. 청크가 생성되었는지 확인
        assertThat(reindexedChunkCount).isGreaterThan(0)

        // 2. 기존 청크 ID가 남아있지 않은지 확인 (완전 삭제 검증)
        val initialChunkIds = initialChunks.map { it.id }.toSet()
        val reindexedChunkIds = reindexedChunks.map { it.id }.toSet()
        val orphanChunks = initialChunkIds.intersect(reindexedChunkIds)
        assertThat(orphanChunks).isEmpty() // 고아 청크 0개
        println("✅ [고아 청크] 발생률: 0% (${orphanChunks.size}개)")

        // 3. 새 청크의 내용이 재인덱싱 요청과 일치하는지 확인
        val newDescriptionChunks = reindexedChunks.filter { it.fieldName == "description" }
        assertThat(newDescriptionChunks).isNotEmpty()
        assertThat(newDescriptionChunks.first().chunkText).contains("Snapdragon")
    }

    @Test
    @DisplayName("시나리오 2: 레코드 삭제 시 해당 레코드의 모든 청크가 함께 삭제된다")
    fun `should delete all chunks when record is deleted`() {
        // given - 테스트 레코드 인덱싱
        val recordKey = "product-002"
        val request = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey,
            fields = mapOf(
                "name" to "노트북 맥북",
                "description" to "Apple M2 칩 탑재 노트북. 13인치 Retina 디스플레이."
            )
        )

        universalVectorIndexingService.indexEntity(request)
        Thread.sleep(1500) // 비동기 벡터 생성 대기

        // 청크가 생성되었는지 확인
        val chunksBeforeDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey
        )
        val chunkCountBeforeDelete = chunksBeforeDelete.size
        assertThat(chunkCountBeforeDelete).isGreaterThan(0)
        println("🔵 [삭제 전] 청크 수: $chunkCountBeforeDelete")

        // when - 레코드 삭제
        universalVectorIndexingService.deleteEntity(testNamespace, testEntity, recordKey)

        // then - 모든 청크가 삭제되었는지 확인
        val chunksAfterDelete = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey
        )
        assertThat(chunksAfterDelete).isEmpty()
        println("✅ [삭제 후] 청크 수: 0개 (모든 청크 정리 완료)")
    }

    @Test
    @DisplayName("시나리오 3: 여러 레코드 재인덱싱 시에도 레코드 간 청크 혼재 없음")
    fun `should not mix chunks between different records during reindexing`() {
        // given - 2개의 서로 다른 레코드 인덱싱
        val recordKey1 = "product-003"
        val recordKey2 = "product-004"

        val request1 = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey1,
            fields = mapOf(
                "name" to "스마트워치 애플워치",
                "description" to "건강 모니터링 기능이 있는 스마트워치"
            )
        )

        val request2 = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey2,
            fields = mapOf(
                "name" to "태블릿 아이패드",
                "description" to "11인치 Liquid Retina 디스플레이 태블릿"
            )
        )

        universalVectorIndexingService.indexEntity(request1)
        universalVectorIndexingService.indexEntity(request2)
        Thread.sleep(2000) // 비동기 벡터 생성 대기

        // 초기 청크 수 확인
        val chunks1Before = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey1
        )
        val chunks2Before = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey2
        )
        println("🔵 [초기] product-003 청크: ${chunks1Before.size}개, product-004 청크: ${chunks2Before.size}개")

        // when - recordKey1만 재인덱싱
        val reindexRequest1 = VectorIndexRequest(
            namespace = testNamespace,
            entity = testEntity,
            recordKey = recordKey1,
            fields = mapOf(
                "name" to "스마트워치 애플워치 Series 9",
                "description" to "최신 건강 모니터링 기능과 ECG 센서가 탑재된 스마트워치입니다."
            )
        )

        universalVectorIndexingService.reindexEntity(reindexRequest1)
        Thread.sleep(2000) // 비동기 벡터 생성 대기

        // then - recordKey1만 변경되고 recordKey2는 그대로인지 확인
        val chunks1After = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey1
        )
        val chunks2After = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            testNamespace, testEntity, recordKey2
        )
        println("🟢 [재인덱싱 후] product-003 청크: ${chunks1After.size}개, product-004 청크: ${chunks2After.size}개")

        // 1. recordKey1 청크가 새로 생성되었는지 확인
        val ids1Before = chunks1Before.map { it.id }.toSet()
        val ids1After = chunks1After.map { it.id }.toSet()
        assertThat(ids1Before.intersect(ids1After)).isEmpty() // 기존 청크 완전 삭제
        println("✅ [product-003] 기존 청크 완전 삭제: ${ids1Before.size}개 → 새 청크 ${ids1After.size}개")

        // 2. recordKey2 청크는 변경되지 않았는지 확인
        val ids2Before = chunks2Before.map { it.id }.toSet()
        val ids2After = chunks2After.map { it.id }.toSet()
        assertThat(ids2Before).isEqualTo(ids2After) // 변경 없음
        println("✅ [product-004] 청크 변경 없음: ${ids2Before.size}개")

        // 3. recordKey1의 새 청크가 recordKey2의 내용을 포함하지 않는지 확인
        val allTexts1 = chunks1After.joinToString(" ") { it.chunkText }
        assertThat(allTexts1).doesNotContain("아이패드")
        assertThat(allTexts1).doesNotContain("태블릿")
        assertThat(allTexts1).contains("애플워치")
        println("✅ [혼재 검증] product-003과 product-004 청크가 혼재되지 않음")
    }

}
