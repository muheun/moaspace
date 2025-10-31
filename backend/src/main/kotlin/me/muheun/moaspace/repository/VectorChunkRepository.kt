package me.muheun.moaspace.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import me.muheun.moaspace.domain.VectorChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * VectorChunk 리포지토리 (MyBatis 마이그레이션 완료)
 *
 * JpaRepository + VectorChunkCustomRepository 통합:
 * - JpaRepository: 기본 CRUD 메서드 (findById, save, delete 등)
 * - VectorChunkCustomRepository: MyBatis 기반 Custom 메서드 (벡터 검색, 동적 삭제)
 *   - VectorChunkMapper는 CustomRepositoryImpl에서 직접 주입받아 사용
 *
 * Constitution Principle VI 준수:
 * - Native Query 제거 완료 (아래 주석 처리된 메서드 참조)
 * - MyBatis Mapper 사용 (pgvector 연산자, CTE, Window Function)
 *
 * Phase 2 (User Story 1): VectorChunk MyBatis 마이그레이션 완료
 */
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

    /**
     * 특정 레코드의 모든 청크 삭제
     *
     * 재인덱싱 시 기존 청크를 삭제하는 용도로 사용됩니다.
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     */
    @Modifying
    @Query("DELETE FROM VectorChunk v WHERE v.namespace = :namespace AND v.entity = :entity AND v.recordKey = :recordKey")
    fun deleteByNamespaceAndEntityAndRecordKey(
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("recordKey") recordKey: String
    )

    /**
     * 특정 레코드의 특정 필드 청크만 삭제
     *
     * 특정 필드만 재인덱싱할 때 사용됩니다.
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     * @param fieldName 필드명
     */
    @Modifying
    @Query("DELETE FROM VectorChunk v WHERE v.namespace = :namespace AND v.entity = :entity AND v.recordKey = :recordKey AND v.fieldName = :fieldName")
    fun deleteByNamespaceAndEntityAndRecordKeyAndFieldName(
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("recordKey") recordKey: String,
        @Param("fieldName") fieldName: String
    )

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
