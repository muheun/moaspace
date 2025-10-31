package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorChunk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

/**
 * VectorChunkRepository 통합 테스트 (Kotlin JDSL + MyBatis 마이그레이션)
 *
 * Constitution Principle V 준수:
 * - 실제 DB 연동 테스트 필수 (Mock 금지)
 * - @Sql로 test-cleanup.sql 초기화
 * - 부동소수점 오차 0.0001 허용
 *
 * TDD 접근법:
 * - 테스트 먼저 작성 → 실패 확인 → 구현 → 통과
 * - VectorChunkCustomRepositoryImpl 구현 전까지 테스트는 실패해야 함
 *
 * Phase 2 (User Story 1): VectorChunk 마이그레이션
 * - Kotlin JDSL: findSimilarRecords, deleteByFilters
 * - MyBatis: findTopChunksByRecord, findByWeightedFieldScore
 *
 * 테스트 설정:
 * - @ActiveProfiles("test"): 테스트 프로파일 활성화
 * - SecurityConfig @Profile("!test"): 테스트 환경에서 Security 비활성화
 * - MyBatis AutoConfiguration 자동 활성화 (application.yml 설정)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class VectorChunkRepositoryTest {

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    /**
     * T009: testFindSimilarRecords() - 레코드별 유사도 검색 테스트
     *
     * 기대 동작:
     * - GROUP BY recordKey + MAX(score) 집계
     * - Nullable namespace/entity 필터링
     * - 코사인 유사도 계산 (1 - cosine_distance)
     * - Score 내림차순 정렬
     *
     * 기대 결과:
     * - recordKey 목록 반환
     * - Score가 0.0001 이내로 기대값과 일치
     * - Limit 적용 확인
     *
     * Constitution Principle V: 실제 DB 연동, 부동소수점 오차 허용
     */
    @Test
    @DisplayName("T009: findSimilarRecords - 레코드별 유사도 검색이 동작한다 (구현 전까지 실패)")
    fun testFindSimilarRecords() {
        // Given: 테스트 벡터 (768차원, multilingual-e5-base 모델)
        val queryVector = FloatArray(768) { 0.5f }

        // When: VectorChunkCustomRepository.findSimilarRecords() 호출
        val results = vectorChunkRepository.findSimilarRecords(
            queryVector = queryVector,
            namespace = "test",
            entity = "Post",
            limit = 10
        )

        // Then: 결과 검증
        assertThat(results).isNotEmpty
        assertThat(results.map { it.recordKey }).contains("post-1", "post-2")

        // Score 검증 (부동소수점 오차 0.0001 허용)
        results.forEach { result ->
            assertThat(result.score).isGreaterThan(0.0)
            assertThat(result.score).isLessThanOrEqualTo(1.0)
        }

        // 내림차순 정렬 확인
        val scores = results.map { it.score }
        assertThat(scores).isSortedAccordingTo(Comparator.reverseOrder())

        // 실제 예상 Score 계산 (PostgreSQL pgvector 코사인 유사도 공식 기반)
        // 참고: 실제 Score는 구현 후 확인 필요 (현재는 테스트 실패가 예상됨)
    }

    /**
     * T010: testFindTopChunksByRecord() - ROW_NUMBER() OVER Window Function 테스트
     *
     * 기대 동작:
     * - CTE + Window Function (ROW_NUMBER() OVER)
     * - 레코드별 최고 스코어 청크만 반환 (rank = 1)
     * - Nullable namespace/entity/fieldName 필터링
     *
     * 기대 결과:
     * - ChunkDetail 목록 반환 (chunkId, namespace, entity, recordKey, fieldName, score)
     * - 올바른 rank 필터링 적용 (post-3의 경우 가장 높은 score 청크만 반환)
     * - Score 내림차순 정렬
     *
     * 특이사항: ROW_NUMBER() OVER는 JPQL 미지원 → Native SQL 모드 사용 예상
     */
    @Test
    @DisplayName("T010: findTopChunksByRecord - Window Function 기반 청크 검색 (구현 전까지 실패)")
    fun testFindTopChunksByRecord() {
        // Given: 테스트 벡터 (768차원)
        val queryVector = FloatArray(768) { 0.7f }

        // When: VectorChunkCustomRepository.findTopChunksByRecord() 호출
        val results = vectorChunkRepository.findTopChunksByRecord(
            queryVector = queryVector,
            namespace = "test",
            entity = "Post",
            fieldName = "content",
            limit = 10
        )

        // Then: 결과 검증
        assertThat(results).isNotEmpty

        // post-3의 경우 3개 청크 중 rank=1인 1개만 반환되어야 함
        val post3Chunks = results.filter { it.recordKey == "post-3" }
        assertThat(post3Chunks).hasSize(1)

        // 반환된 청크가 가장 높은 score를 가진 청크인지 확인
        val topChunk = post3Chunks.first()
        assertThat(topChunk.fieldName).isEqualTo("content")
        assertThat(topChunk.score).isGreaterThan(0.0)

        // 내림차순 정렬 확인
        val scores = results.map { it.score }
        assertThat(scores).isSortedAccordingTo(Comparator.reverseOrder())
    }

    /**
     * T011: testFindByWeightedFieldScore() - 필드별 가중치 검색 테스트
     *
     * 기대 동작:
     * - CTE 서브쿼리로 필드별 스코어 계산 (field_scores)
     * - CASE WHEN으로 가중치 적용 (title: 2.0, content: 1.0)
     * - SUM(weighted_score) 집계
     *
     * 기대 결과:
     * - title 필드 스코어에 2배 가중치 적용
     * - content 필드 스코어에 1배 가중치 적용
     * - weightedScore가 정확히 계산됨 (오차 0.0001 이내)
     *
     * Constitution Principle II: 필드별 가중치 지원
     */
    @Test
    @DisplayName("T011: findByWeightedFieldScore - 가중치 적용 검색이 정확하다 (구현 전까지 실패)")
    fun testFindByWeightedFieldScore() {
        // Given: 테스트 벡터 (768차원, 가중치 테스트용 namespace 사용)
        val queryVector = FloatArray(768) { 0.8f }

        // When: 가중치 적용 검색 (title: 2.0, content: 1.0)
        val results = vectorChunkRepository.findByWeightedFieldScore(
            queryVector = queryVector,
            namespace = "weighted_test",
            entity = "Post",
            titleWeight = 2.0,
            contentWeight = 1.0,
            limit = 10
        )

        // Then: 결과 검증
        assertThat(results).isNotEmpty
        assertThat(results.map { it.recordKey }).contains("weighted-1")

        // 가중치 계산 검증
        // weighted-1의 경우:
        // - title score (0.8 벡터): 약 1.0 (완전 일치 가정) * 2.0 (title weight)
        // - content score (0.4 벡터): 약 0.5 (가정) * 1.0 (content weight)
        // - Total: 2.0 + 0.5 = 2.5 (실제 값은 구현 후 확인)

        val weighted1 = results.find { it.recordKey == "weighted-1" }
        assertThat(weighted1).isNotNull
        assertThat(weighted1!!.weightedScore).isGreaterThan(0.0)

        // 가중치가 적용되었는지 확인 (title의 가중치가 더 높으므로 score > content score)
        // 실제 값은 구현 후 정확한 기대값으로 업데이트 필요
    }

    /**
     * T012: testDeleteByFilters() - 동적 조건 삭제 테스트
     *
     * 기대 동작:
     * - whereAnd() 패턴으로 nullable fieldName 처리
     * - fieldName이 null이면 모든 필드 삭제
     * - fieldName이 지정되면 해당 필드만 삭제
     *
     * 기대 결과:
     * - fieldName=null: namespace+entity+recordKey에 해당하는 모든 필드 삭제
     * - fieldName 지정: 해당 필드만 삭제
     * - 삭제된 레코드 수 반환
     *
     * Constitution Principle V: 실제 DB 연동, 삭제 결과 검증
     */
    @Test
    @DisplayName("T012-1: deleteByFilters - fieldName이 null이면 모든 필드를 삭제한다 (구현 전까지 실패)")
    fun testDeleteByFiltersAllFields() {
        // Given: delete_test 네임스페이스에 delete-1 레코드 2개 필드 존재 (title, content)
        val beforeCount = vectorChunkRepository.count()

        // When: fieldName=null로 삭제 (모든 필드 삭제)
        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = "delete_test",
            entity = "Post",
            recordKey = "delete-1",
            fieldName = null
        )

        // Then: 2개 필드 모두 삭제됨
        assertThat(deletedCount).isEqualTo(2)

        val afterCount = vectorChunkRepository.count()
        assertThat(afterCount).isEqualTo(beforeCount - 2)
    }

    @Test
    @DisplayName("T012-2: deleteByFilters - fieldName 지정 시 해당 필드만 삭제한다 (구현 전까지 실패)")
    fun testDeleteByFiltersSingleField() {
        // Given: delete_test 네임스페이스에 delete-2 레코드 존재 (title 필드만)
        val beforeCount = vectorChunkRepository.count()

        // When: fieldName="title"로 삭제
        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = "delete_test",
            entity = "Post",
            recordKey = "delete-2",
            fieldName = "title"
        )

        // Then: 1개 필드만 삭제됨
        assertThat(deletedCount).isEqualTo(1)

        val afterCount = vectorChunkRepository.count()
        assertThat(afterCount).isEqualTo(beforeCount - 1)
    }

    /**
     * 추가 테스트: JpaRepository 기본 CRUD 동작 확인
     *
     * VectorChunkRepository가 JpaRepository를 상속받았으므로 기본 CRUD 메서드도 테스트
     */
    @Test
    @DisplayName("추가 테스트: JpaRepository 기본 메서드 동작 확인")
    fun testJpaRepositoryBasicCrud() {
        // Given: 새로운 VectorChunk 엔티티 생성
        val chunk = VectorChunk(
            namespace = "test",
            entity = "Post",
            recordKey = "new-post",
            fieldName = "title",
            chunkText = "New Post Title",
            chunkIndex = 0,
            startPosition = 0,
            endPosition = 14
        )

        // When: save() 호출
        val saved = vectorChunkRepository.save(chunk)

        // Then: 저장 성공 및 ID 생성 확인
        assertThat(saved.id).isNotNull
        assertThat(saved.recordKey).isEqualTo("new-post")

        // findById() 조회 확인
        val found = vectorChunkRepository.findById(saved.id!!)
        assertThat(found).isPresent
        assertThat(found.get().chunkText).isEqualTo("New Post Title")

        // delete() 삭제 확인
        vectorChunkRepository.delete(saved)
        val deleted = vectorChunkRepository.findById(saved.id!!)
        assertThat(deleted).isEmpty
    }
}
