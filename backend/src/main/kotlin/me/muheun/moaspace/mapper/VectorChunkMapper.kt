package me.muheun.moaspace.mapper

import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * VectorChunk MyBatis Mapper 인터페이스
 *
 * **Constitution Principle VI 준수 - JPQL 미지원 쿼리 처리**:
 * - Kotlin JDSL: JPQL 지원 쿼리 (GROUP BY, JOIN, WHERE 등)
 * - MyBatis: JPQL 미지원 쿼리 (CTE, Window Function, pgvector 연산자)
 *
 * **이 Mapper가 필요한 이유**:
 * 1. `findSimilarRecords`: pgvector `<=>` 연산자 - JPQL 미지원
 * 2. `findTopChunksByRecord`: ROW_NUMBER() OVER (PARTITION BY) - JPQL 미지원
 * 3. `findByWeightedFieldScore`: CTE (WITH ...) - JPQL 미지원
 *
 * **XML Mapping**: `resources/mapper/VectorChunkMapper.xml`
 *
 * @see me.muheun.moaspace.repository.VectorChunkCustomRepository
 */
@Mapper
interface VectorChunkMapper {

    /**
     * 레코드별 유사도 검색 (GROUP BY + MAX aggregation)
     *
     * **MyBatis 사용 이유**: pgvector `<=>` 연산자는 JPQL 미지원
     *
     * SQL 구조:
     * ```sql
     * SELECT record_key,
     *        MAX(1 - (chunk_vector <=> CAST(:queryVector AS vector))) as score
     * FROM vector_chunk
     * WHERE chunk_vector IS NOT NULL
     *   AND namespace = :namespace (nullable)
     *   AND entity = :entity (nullable)
     * GROUP BY record_key
     * ORDER BY score DESC
     * LIMIT :limit
     * ```
     *
     * **핵심 기능**:
     * - pgvector 코사인 유사도 계산: `1 - (chunk_vector <=> query_vector)`
     * - 레코드별 최고 스코어만 반환 (MAX aggregation)
     * - Nullable 필터링 (namespace, entity)
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param limit 결과 개수 제한
     * @return 레코드별 유사도 스코어 목록
     */
    fun findSimilarRecords(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("limit") limit: Int
    ): List<RecordSimilarityScore>

    /**
     * 청크 상세 정보 포함 검색 (CTE + Window Function)
     *
     * **MyBatis 사용 이유**: ROW_NUMBER() OVER는 JPQL 미지원
     *
     * SQL 구조:
     * ```sql
     * WITH ranked_chunks AS (
     *     SELECT ...,
     *            ROW_NUMBER() OVER (PARTITION BY namespace, entity, record_key
     *                               ORDER BY score DESC) as rank
     *     FROM vector_chunk
     *     WHERE ...
     * )
     * SELECT * FROM ranked_chunks WHERE rank = 1
     * ORDER BY score DESC LIMIT :limit
     * ```
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param fieldName 필드명 필터 (nullable)
     * @param limit 결과 개수 제한
     * @return 레코드별 최고 스코어 청크 목록
     */
    fun findTopChunksByRecord(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("fieldName") fieldName: String?,
        @Param("limit") limit: Int
    ): List<ChunkDetail>

    /**
     * 필드별 가중치 벡터 검색 (CTE + CASE WHEN)
     *
     * **MyBatis 사용 이유**: CTE (WITH ...)는 JPQL 미지원
     *
     * SQL 구조:
     * ```sql
     * WITH field_scores AS (
     *     SELECT record_key, field_name, MAX(score) as score
     *     FROM vector_chunk
     *     WHERE ...
     *     GROUP BY record_key, field_name
     * )
     * SELECT record_key,
     *        SUM(CASE
     *            WHEN field_name = 'title' THEN score * :titleWeight
     *            WHEN field_name = 'content' THEN score * :contentWeight
     *        END) as weightedScore
     * FROM field_scores
     * GROUP BY record_key
     * ORDER BY weightedScore DESC LIMIT :limit
     * ```
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
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("titleWeight") titleWeight: Double,
        @Param("contentWeight") contentWeight: Double,
        @Param("limit") limit: Int
    ): List<WeightedScore>

    /**
     * 조건별 청크 삭제 (동적 필터링)
     *
     * **MyBatis 사용 이유**: 코드에 쿼리 문자열 Zero (Constitution VI v2.5.0)
     *
     * SQL 구조:
     * ```sql
     * DELETE FROM vector_chunk
     * WHERE namespace = :namespace
     *   AND entity = :entity
     *   AND record_key = :recordKey
     *   AND field_name = :fieldName (nullable - 동적 조건)
     * ```
     *
     * **핵심 기능**:
     * - fieldName이 null이면 모든 필드 삭제 (namespace + entity + recordKey로 필터링)
     * - fieldName이 지정되면 해당 필드만 삭제
     * - 동적 SQL로 nullable 파라미터 처리
     *
     * @param namespace 네임스페이스 (필수)
     * @param entity 엔티티 (필수)
     * @param recordKey 레코드 키 (필수)
     * @param fieldName 필드명 (nullable - null이면 모든 필드 삭제)
     * @return 삭제된 레코드 수
     */
    fun deleteByFilters(
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("recordKey") recordKey: String,
        @Param("fieldName") fieldName: String?
    ): Int
}
