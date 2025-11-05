package me.muheun.moaspace.repository

import com.pgvector.PGvector
import me.muheun.moaspace.domain.vector.VectorChunk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

/**
 * VectorChunkRepository 통합 테스트
 *
 * Constitution Principle V 준수:
 * - 실제 DB 연동 테스트 필수 (Mock 금지)
 * - @Sql로 test-cleanup.sql 초기화
 * - 부동소수점 오차 0.0001 허용
 *
 * 테스트 설정:
 * - @ActiveProfiles("test"): 테스트 프로파일 활성화
 * - @SpringBootTest: 전체 ApplicationContext 로드 (MyBatis + QueryDSL + JPA 모두 활성화)
 * - @Transactional: 각 테스트 후 자동 롤백
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class VectorChunkRepositoryTest {

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    /**
     * 테스트용 벡터 데이터 생성
     *
     * 3개 레코드 (post-1, post-2, post-3) 각각 title/content 필드로 총 6개 청크
     */
    private fun createTestData() {
        val sampleVectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f) + FloatArray(765) { 0.0f }, // post-1 title
            floatArrayOf(0.2f, 0.3f, 0.4f) + FloatArray(765) { 0.0f }, // post-1 content
            floatArrayOf(0.3f, 0.4f, 0.5f) + FloatArray(765) { 0.0f }, // post-2 title
            floatArrayOf(0.4f, 0.5f, 0.6f) + FloatArray(765) { 0.0f }, // post-2 content
            floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }, // post-3 title (높은 유사도)
            floatArrayOf(0.5f, 0.6f, 0.7f) + FloatArray(765) { 0.0f }  // post-3 content
        )

        val chunks = listOf(
            VectorChunk(namespace = "posts", entity = "post", recordKey = "1", fieldName = "title", chunkText = "Title 1", chunkVector = PGvector(sampleVectors[0]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "1", fieldName = "content", chunkText = "Content 1", chunkVector = PGvector(sampleVectors[1]), chunkIndex = 1, startPosition = 11, endPosition = 30),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "2", fieldName = "title", chunkText = "Title 2", chunkVector = PGvector(sampleVectors[2]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "2", fieldName = "content", chunkText = "Content 2", chunkVector = PGvector(sampleVectors[3]), chunkIndex = 1, startPosition = 11, endPosition = 30),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "3", fieldName = "title", chunkText = "Title 3", chunkVector = PGvector(sampleVectors[4]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "3", fieldName = "content", chunkText = "Content 3", chunkVector = PGvector(sampleVectors[5]), chunkIndex = 1, startPosition = 11, endPosition = 30)
        )

        vectorChunkRepository.saveAll(chunks)
    }

    /**
     * T011: findSimilarRecords() 테스트
     *
     * 기대 동작:
     * - MyBatis Mapper 호출: findSimilarRecords()
     * - GROUP BY recordKey: 레코드별 최대 유사도 집계
     * - 결과: RecordSimilarityScore 목록 (recordKey, score)
     * - 스코어 검증: 0.0001 이내 오차 허용
     */
    @Test
    fun `testFindSimilarRecords - 레코드별 유사도 검색`() {
        // Given: 테스트 데이터 생성
        createTestData()

        // 쿼리 벡터 (post-3 title과 유사)
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        // When: findSimilarRecords() 호출
        val results = vectorChunkRepository.findSimilarRecords(queryVector, "posts", "post", 3)

        // Then: 결과 검증
        assertThat(results).hasSize(3)
        assertThat(results[0].recordKey).isEqualTo("3") // 가장 유사한 레코드
        assertThat(results[0].score).isGreaterThan(0.9) // 높은 유사도
    }

    /**
     * T012: findTopChunksByRecord() 테스트
     *
     * 기대 동작:
     * - MyBatis Mapper 호출: findTopChunksByRecord()
     * - CTE + ROW_NUMBER() OVER PARTITION BY: 레코드별 최상위 1개 청크 추출
     * - 결과: ChunkDetail 목록 (chunkId, namespace, entity, recordKey, fieldName, score)
     */
    @Test
    fun `testFindTopChunksByRecord - 레코드별 최상위 청크 검색`() {
        // Given: 테스트 데이터 생성
        createTestData()

        // 쿼리 벡터
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        // When: findTopChunksByRecord() 호출
        val results = vectorChunkRepository.findTopChunksByRecord(queryVector, "posts", "post", null, 3)

        // Then: 결과 검증 (레코드당 1개씩만 반환)
        assertThat(results).hasSize(3)
        assertThat(results.map { it.recordKey }.distinct()).hasSize(3) // 중복 없음
    }

    /**
     * T013: findByWeightedFieldScore() 테스트
     *
     * 기대 동작:
     * - MyBatis Mapper 호출: findByWeightedFieldScore()
     * - CTE + CASE WHEN: 필드별 가중치 적용 (title: 0.7, content: 0.3)
     * - 결과: WeightedScore 목록 (recordKey, fieldName, weightedScore)
     */
    @Test
    fun `testFindByWeightedFieldScore - 필드별 가중치 적용 검색`() {
        // Given: 테스트 데이터 생성
        createTestData()

        // 쿼리 벡터
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        // When: findByWeightedFieldScore() 호출
        val results = vectorChunkRepository.findByWeightedFieldScore(queryVector, "posts", "post", 0.7, 0.3, 10)

        // Then: 결과 검증 (title 필드가 더 높은 가중치)
        assertThat(results).isNotEmpty
        val titleScore = results.find { it.fieldName == "title" }?.weightedScore ?: 0.0
        val contentScore = results.find { it.fieldName == "content" }?.weightedScore ?: 0.0
        assertThat(titleScore).isGreaterThan(contentScore) // title 가중치가 더 높음
    }

    /**
     * T014: deleteByFilters() 테스트
     *
     * 기대 동작:
     * - MyBatis Mapper 호출: deleteByFilters()
     * - 동적 DELETE: fieldName이 null이면 모든 필드 삭제, 아니면 특정 필드만 삭제
     * - 결과: 삭제된 행 개수
     */
    @Test
    fun `testDeleteByFilters - nullable fieldName 처리`() {
        // Given: 테스트 데이터 생성
        createTestData()

        // When: fieldName = null (post-1의 모든 필드 삭제)
        val deletedAll = vectorChunkRepository.deleteByFilters("posts", "post", "1", null)

        // Then: 2개 삭제 (title + content)
        assertThat(deletedAll).isEqualTo(2)

        // When: fieldName = "title" (post-2의 title만 삭제)
        val deletedTitle = vectorChunkRepository.deleteByFilters("posts", "post", "2", "title")

        // Then: 1개 삭제 (title만)
        assertThat(deletedTitle).isEqualTo(1)

        // 최종 검증: post-1 (0개), post-2 (1개 - content만 남음), post-3 (2개)
        val remaining = vectorChunkRepository.findAll()
        assertThat(remaining).hasSize(3) // 6 - 2 - 1 = 3
    }
}
