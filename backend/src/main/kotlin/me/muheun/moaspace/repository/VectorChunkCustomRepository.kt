package me.muheun.moaspace.repository

import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore

/**
 * VectorChunk 엔티티에 대한 Kotlin JDSL 기반 Custom Repository 인터페이스
 *
 * Constitution Principle VI 준수:
 * - Native Query 제거
 * - Kotlin JDSL로 타입 안전 쿼리 작성
 * - pgvector 연산자는 VectorJpql Custom DSL로 래핑
 *
 * JPA CustomRepository 패턴:
 * 1. 이 인터페이스: 순수 메서드 시그니처 정의 (계약)
 * 2. VectorChunkCustomRepositoryImpl: 실제 Kotlin JDSL 쿼리 구현
 * 3. VectorChunkRepository: JpaRepository + Custom 통합
 *
 * Phase: 1 (최우선 마이그레이션 대상)
 */
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

    /**
     * 청크 상세 정보 포함 검색 (CTE + Window Function)
     *
     * 기존 Native Query:
     * ```sql
     * WITH ranked_chunks AS (
     *     SELECT v.id, v.namespace, v.entity, v.record_key, v.field_name,
     *            1 - (v.chunk_vector <=> CAST(:queryVector AS vector)) as score,
     *            ROW_NUMBER() OVER (PARTITION BY v.namespace, v.entity, v.record_key
     *                               ORDER BY (1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) DESC) as rank
     *     FROM vector_chunk v
     *     WHERE v.chunk_vector IS NOT NULL
     *       AND (:namespace IS NULL OR v.namespace = :namespace)
     *       AND (:entity IS NULL OR v.entity = :entity)
     *       AND (:fieldName IS NULL OR v.field_name = :fieldName)
     * )
     * SELECT id as chunkId, namespace, entity, record_key as recordKey, field_name as fieldName, score
     * FROM ranked_chunks
     * WHERE rank = 1
     * ORDER BY score DESC
     * LIMIT :limit
     * ```
     *
     * Kotlin JDSL 변환: Native SQL 모드 사용 (ROW_NUMBER() OVER는 JPQL 미지원)
     * 주석으로 사유 명시 필요 (Constitution Principle VI 예외)
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param fieldName 필드명 필터 (nullable)
     * @param limit 결과 개수 제한
     * @return 레코드별 최고 스코어 청크 목록
     */
    fun findTopChunksByRecord(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        fieldName: String?,
        limit: Int
    ): List<ChunkDetail>

    /**
     * 필드별 가중치 벡터 검색
     *
     * 기존 Native Query:
     * ```sql
     * WITH field_scores AS (
     *     SELECT v.record_key, v.field_name,
     *            MAX(1 - (v.chunk_vector <=> CAST(:queryVector AS vector))) as score
     *     FROM vector_chunk v
     *     WHERE v.chunk_vector IS NOT NULL
     *       AND v.namespace = :namespace
     *       AND v.entity = :entity
     *       AND v.field_name IN ('title', 'content')
     *     GROUP BY v.record_key, v.field_name
     * )
     * SELECT record_key as recordKey,
     *        SUM(CASE
     *            WHEN field_name = 'title' THEN score * :titleWeight
     *            WHEN field_name = 'content' THEN score * :contentWeight
     *            ELSE score
     *        END) as weightedScore
     * FROM field_scores
     * GROUP BY record_key
     * ORDER BY weightedScore DESC
     * LIMIT :limit
     * ```
     *
     * Kotlin JDSL 변환: 서브쿼리 + CASE WHEN 표현식
     * (Constitution Principle II 필드별 가중치 지원)
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param namespace 네임스페이스 (필수)
     * @param entity 엔티티 (필수)
     * @param titleWeight 제목 가중치 (기본값 2.0)
     * @param contentWeight 내용 가중치 (기본값 1.0)
     * @param limit 결과 개수 제한
     * @return 가중치 적용 스코어 목록
     */
    fun findByWeightedFieldScore(
        queryVector: FloatArray,
        namespace: String,
        entity: String,
        titleWeight: Double = 2.0,
        contentWeight: Double = 1.0,
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
