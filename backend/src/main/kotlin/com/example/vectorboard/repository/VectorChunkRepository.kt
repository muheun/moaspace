package com.example.vectorboard.repository

import com.example.vectorboard.domain.VectorChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * VectorChunk 리포지토리
 *
 * 범용 벡터 청크 CRUD 및 검색 기능을 제공합니다.
 * namespace/entity/recordKey/fieldName 조합으로 모든 엔티티를 지원합니다.
 */
@Repository
interface VectorChunkRepository : JpaRepository<VectorChunk, Long> {

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
    // 벡터 유사도 검색 메서드
    // ========================================

    /**
     * 벡터 유사도 기반 검색 (레코드별 그룹화)
     *
     * 각 레코드별로 가장 유사도가 높은 청크의 점수를 사용하여 정렬합니다.
     * namespace와 entity로 검색 범위를 제한할 수 있습니다.
     *
     * @param queryVector 검색 쿼리의 벡터 (문자열 형식, 예: "[0.1, 0.2, ...]")
     * @param namespace 네임스페이스 (null이면 전체)
     * @param entity 엔티티 타입 (null이면 전체)
     * @param limit 반환할 레코드 개수
     * @return 레코드 키와 유사도 점수 쌍의 리스트
     */
    @Query(
        value = """
            SELECT
                v.record_key as recordKey,
                MAX(1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) as score
            FROM vector_chunk v
            WHERE v.chunk_vector IS NOT NULL
                AND (:namespace IS NULL OR v.namespace = :namespace)
                AND (:entity IS NULL OR v.entity = :entity)
            GROUP BY v.record_key
            ORDER BY score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarRecords(
        @Param("queryVector") queryVector: String,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("limit") limit: Int
    ): List<RecordSimilarityScore>

    /**
     * 벡터 유사도 기반 검색 (청크 상세 정보 포함)
     *
     * 각 레코드별로 가장 유사도가 높은 청크 1개와 함께 반환합니다.
     * fieldName 필터링을 지원하여 특정 필드에서만 검색할 수 있습니다.
     *
     * @param queryVector 검색 쿼리의 벡터
     * @param namespace 네임스페이스 (null이면 전체)
     * @param entity 엔티티 타입 (null이면 전체)
     * @param fieldName 필드명 (null이면 전체 필드)
     * @param limit 반환할 결과 개수
     * @return 청크 ID, 레코드 키, 필드명, 유사도 점수를 포함한 결과
     */
    @Query(
        value = """
            WITH ranked_chunks AS (
                SELECT
                    v.id,
                    v.namespace,
                    v.entity,
                    v.record_key,
                    v.field_name,
                    1 - (v.chunk_vector <=> CAST(:queryVector AS vector)) as score,
                    ROW_NUMBER() OVER (
                        PARTITION BY v.namespace, v.entity, v.record_key
                        ORDER BY (1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) DESC
                    ) as rank
                FROM vector_chunk v
                WHERE v.chunk_vector IS NOT NULL
                    AND (:namespace IS NULL OR v.namespace = :namespace)
                    AND (:entity IS NULL OR v.entity = :entity)
                    AND (:fieldName IS NULL OR v.field_name = :fieldName)
            )
            SELECT
                id as chunkId,
                namespace,
                entity,
                record_key as recordKey,
                field_name as fieldName,
                score
            FROM ranked_chunks
            WHERE rank = 1
            ORDER BY score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTopChunksByRecord(
        @Param("queryVector") queryVector: String,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("fieldName") fieldName: String?,
        @Param("limit") limit: Int
    ): List<VectorChunkSearchResult>

    /**
     * 필드별 가중치를 적용한 벡터 검색
     *
     * 여러 필드에 서로 다른 가중치를 적용하여 검색합니다.
     * 예: title 60%, content 40%
     *
     * @param queryVector 검색 쿼리의 벡터
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param titleWeight title 필드 가중치 (0.0 ~ 1.0)
     * @param contentWeight content 필드 가중치 (0.0 ~ 1.0)
     * @param limit 반환할 결과 개수
     * @return 가중 평균 점수로 정렬된 결과
     */
    @Query(
        value = """
            WITH field_scores AS (
                SELECT
                    v.record_key,
                    v.field_name,
                    MAX(1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) as score
                FROM vector_chunk v
                WHERE v.chunk_vector IS NOT NULL
                    AND v.namespace = :namespace
                    AND v.entity = :entity
                    AND v.field_name IN ('title', 'content')
                GROUP BY v.record_key, v.field_name
            )
            SELECT
                record_key as recordKey,
                SUM(
                    CASE
                        WHEN field_name = 'title' THEN score * :titleWeight
                        WHEN field_name = 'content' THEN score * :contentWeight
                        ELSE score
                    END
                ) as weightedScore
            FROM field_scores
            GROUP BY record_key
            ORDER BY weightedScore DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findByWeightedFieldScore(
        @Param("queryVector") queryVector: String,
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("titleWeight") titleWeight: Double,
        @Param("contentWeight") contentWeight: Double,
        @Param("limit") limit: Int
    ): List<WeightedSearchResult>
}

// ========================================
// 네이티브 쿼리 결과 매핑 인터페이스
// ========================================

/**
 * 레코드 유사도 점수 인터페이스
 */
interface RecordSimilarityScore {
    fun getRecordKey(): String
    fun getScore(): Double
}

/**
 * 벡터 청크 검색 결과 인터페이스
 */
interface VectorChunkSearchResult {
    fun getChunkId(): Long
    fun getNamespace(): String
    fun getEntity(): String
    fun getRecordKey(): String
    fun getFieldName(): String
    fun getScore(): Double
}

/**
 * 가중치 검색 결과 인터페이스
 */
interface WeightedSearchResult {
    fun getRecordKey(): String
    fun getWeightedScore(): Double
}
