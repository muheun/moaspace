package me.muheun.moaspace.repository

import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore

interface VectorChunkCustomRepository {

    /**
     * 레코드별 유사도 검색 (GROUP BY + 집계)
     *
     * 기존 Native Query:
     * ```sql
     * SELECT v.record_key as recordKey,
     *        MAX(1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) as score
     * FROM vector_chunk v
     * WHERE v.chunk_vector IS NOT NULL
     *   AND (:namespace IS NULL OR v.namespace = :namespace)
     *   AND (:entity IS NULL OR v.entity = :entity)
     * GROUP BY v.record_key
     * ORDER BY score DESC
     * LIMIT :limit
     * ```
     *
     * Kotlin JDSL 변환: VectorJpql.cosineSimilarity() 사용
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param namespace 네임스페이스 필터 (nullable, null이면 전체 검색)
     * @param entity 엔티티 필터 (nullable, null이면 전체 검색)
     * @param limit 결과 개수 제한
     * @return 레코드별 최대 유사도 스코어 목록
     */
    fun findSimilarRecords(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        limit: Int
    ): List<RecordSimilarityScore>

    
    fun findTopChunksByRecord(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        fieldName: String?,
        limit: Int
    ): List<ChunkDetail>

    
    fun findByWeightedFieldScore(
        queryVector: FloatArray,
        namespace: String,
        entity: String,
        limit: Int
    ): List<WeightedScore>

    /**
     * 동적 조건 조합 삭제
     *
     * 기존 Native Query:
     * ```kotlin
     * @Modifying
     * @Query("DELETE FROM VectorChunk v WHERE v.namespace = :namespace AND v.entity = :entity AND v.recordKey = :recordKey")
     * fun deleteByNamespaceAndEntityAndRecordKey(...)
     *
     * @Modifying
     * @Query("DELETE FROM VectorChunk v WHERE v.namespace = :namespace AND v.entity = :entity AND v.recordKey = :recordKey AND v.fieldName = :fieldName")
     * fun deleteByNamespaceAndEntityAndRecordKeyAndFieldName(...)
     * ```
     *
     * Kotlin JDSL 변환: whereAnd() 패턴으로 통합
     *
     * @param namespace 네임스페이스 (필수)
     * @param entity 엔티티 (필수)
     * @param recordKey 레코드 키 (필수)
     * @param fieldName 필드명 (nullable, null이면 모든 필드 삭제)
     * @return 삭제된 레코드 수
     */
    fun deleteByFilters(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String?
    ): Int
}
