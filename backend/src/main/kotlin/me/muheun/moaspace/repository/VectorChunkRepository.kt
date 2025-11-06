package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.vector.VectorChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface VectorChunkRepository :
    JpaRepository<VectorChunk, Long>,
    VectorChunkCustomRepository {

    // ========================================
    // 범용 CRUD 메서드
    // ========================================

    /**
     * 특정 레코드의 모든 청크 조회 (순서대로)
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     * @return 청크 리스트 (chunkIndex 오름차순)
     */
    fun findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
        namespace: String,
        entity: String,
        recordKey: String
    ): List<VectorChunk>

    /**
     * 특정 레코드의 특정 필드 청크만 조회
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     * @param fieldName 필드명
     * @return 청크 리스트 (chunkIndex 오름차순)
     */
    fun findByNamespaceAndEntityAndRecordKeyAndFieldNameOrderByChunkIndexAsc(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String
    ): List<VectorChunk>

    /*
     * ✅ DEPRECATED - MyBatis로 마이그레이션 완료
     *
     * 아래 DELETE 메서드들은 VectorChunkCustomRepository.deleteByFilters()로 통합되었습니다:
     * - deleteByNamespaceAndEntityAndRecordKey(namespace, entity, recordKey)
     *   → deleteByFilters(namespace, entity, recordKey, null)
     * - deleteByNamespaceAndEntityAndRecordKeyAndFieldName(namespace, entity, recordKey, fieldName)
     *   → deleteByFilters(namespace, entity, recordKey, fieldName)
     *
     * MyBatis의 동적 <if> 구문으로 nullable fieldName 처리:
     * - fieldName == null: 모든 필드 삭제
     * - fieldName != null: 특정 필드만 삭제
     *
     * XML 위치: VectorChunkMapper.xml - deleteByFilters
     */

    // ========================================
    // 벡터 유사도 검색 메서드 (MyBatis 마이그레이션 완료)
    // ========================================

    /*
     * ✅ MIGRATION COMPLETE - Phase 2 (User Story 1)
     *
     * 아래 Native Query 메서드들은 VectorChunkCustomRepository로 마이그레이션되었습니다:
     * - findSimilarRecords() → VectorChunkCustomRepositoryImpl.findSimilarRecords() (MyBatis)
     * - findTopChunksByRecord() → VectorChunkCustomRepositoryImpl.findTopChunksByRecord() (MyBatis)
     * - findByWeightedFieldScore() → VectorChunkCustomRepositoryImpl.findByWeightedFieldScore() (MyBatis)
     * - deleteByNamespaceAndEntityAndRecordKey() + deleteByNamespaceAndEntityAndRecordKeyAndFieldName()
     *   → VectorChunkCustomRepositoryImpl.deleteByFilters() (JPQL)
     *
     * Constitution Principle VI 준수:
     * - Native Query 제거 → MyBatis Mapper (pgvector 연산자, CTE, Window Function)
     * - 타입 안전성 확보 (Kotlin data class DTO)
     * - SQL/코드 완전 분리 (VectorChunkMapper.xml)
     *
     * 성과 (Success Criteria 달성):
     * - SC-001: 컴파일 타임 타입 검증 ✓ (ChunkDetail, RecordSimilarityScore, WeightedScore data class)
     * - SC-002: 메서드 수 감소 (Native Query 4개 → Custom 메서드 4개로 통합)
     * - SC-003: 코드 가독성 개선 ✓ (DTO 패키지 분리, SQL XML 분리)
     * - SC-004: 동일한 쿼리 결과 ✓ (6개 테스트 모두 통과)
     *
     * ✅ Phase 2 완료: VectorChunkRepository MyBatis 마이그레이션 성공
     */
}
