package me.muheun.moaspace.mapper

import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * VectorChunk MyBatis Mapper 인터페이스
 *
 * QueryDSL/JPQL로 구현 불가능한 쿼리를 MyBatis XML로 처리합니다.
 * Constitution Principle VI v3.0.0 준수: QueryDSL 불가능 → MyBatis로 분리
 *
 * 처리하는 쿼리 타입:
 * - CTE (WITH 절): JPQL 미지원
 * - Window Function (ROW_NUMBER() OVER PARTITION BY): JPQL 미지원
 * - pgvector 연산자 (<=>): QueryDSL template으로 가능하나 MyBatis가 더 적합
 *
 * Mapper XML 위치: backend/src/main/resources/mapper/VectorChunkMapper.xml
 */
@Mapper
interface VectorChunkMapper {

    /**
     * 레코드별 유사도 검색 (pgvector <=> 연산자)
     *
     * pgvector 코사인 거리 연산자 `<=>`를 사용하여 벡터 유사도를 계산합니다.
     * QueryDSL template으로도 가능하지만 복잡한 쿼리는 MyBatis가 가독성이 높습니다.
     *
     * SQL 특징:
     * - GROUP BY recordKey: 레코드별 최대 유사도 집계
     * - pgvector `<=>`: 코사인 거리 (1 - <=> = 코사인 유사도)
     * - Dynamic WHERE: nullable 파라미터 필터링
     *
     * @param queryVector 검색 벡터 (FloatArray → PostgreSQL vector 캐스팅)
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param limit 결과 개수 제한
     * @return 레코드별 최대 유사도 스코어 목록
     */
    fun findSimilarRecords(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("limit") limit: Int
    ): List<RecordSimilarityScore>

    /**
     * 청크 상세 정보 검색 (CTE + Window Function)
     *
     * CTE와 ROW_NUMBER() OVER PARTITION BY를 사용하여 레코드별 최상위 1개 청크를 추출합니다.
     * JPQL은 Window Function을 지원하지 않으므로 MyBatis 필수입니다.
     *
     * SQL 특징:
     * - CTE (WITH ranked_chunks AS): 임시 결과 집합 생성
     * - ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...): 레코드별 랭킹
     * - WHERE rank = 1: 레코드별 최상위 1개만 추출
     *
     * @param queryVector 검색 벡터
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param fieldName 필드명 필터 (nullable)
     * @param limit 결과 개수 제한
     * @return 레코드별 최상위 청크 상세 정보 목록
     */
    fun findTopChunksByRecord(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("fieldName") fieldName: String?,
        @Param("limit") limit: Int
    ): List<ChunkDetail>

    /**
     * 필드별 가중치 적용 검색 (CTE + CASE WHEN)
     *
     * CTE와 CASE WHEN을 사용하여 필드별로 서로 다른 가중치를 적용합니다.
     * JPQL로도 CASE WHEN은 가능하지만 CTE가 필요하므로 MyBatis 사용합니다.
     *
     * SQL 특징:
     * - CTE: 복잡한 계산 결과 집합 생성
     * - CASE WHEN field_name: 필드별 가중치 적용
     * - Dynamic WHERE: nullable 파라미터 필터링
     *
     * @param queryVector 검색 벡터
     * @param namespace 네임스페이스 필터 (nullable)
     * @param entity 엔티티 필터 (nullable)
     * @param titleWeight title 필드 가중치
     * @param contentWeight content 필드 가중치
     * @param limit 결과 개수 제한
     * @return 필드별 가중치가 적용된 스코어 목록
     */
    fun findByWeightedFieldScore(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String?,
        @Param("titleWeight") titleWeight: Double,
        @Param("contentWeight") contentWeight: Double,
        @Param("limit") limit: Int
    ): List<WeightedScore>

    /**
     * 필터 기반 VectorChunk 삭제 (동적 DELETE)
     *
     * nullable fieldName 파라미터에 따라 DELETE 조건을 동적으로 변경합니다.
     * MyBatis의 <if test="fieldName != null"> 구문으로 처리합니다.
     *
     * SQL 특징:
     * - Dynamic DELETE: fieldName이 null이면 해당 조건 무시
     * - Constitution VI 준수: 코드에 DELETE 문자열 없음, XML로 완전 분리
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티
     * @param recordKey 레코드 키
     * @param fieldName 필드명 (nullable, null이면 모든 필드 삭제)
     * @return 삭제된 행 개수
     */
    fun deleteByFilters(
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("recordKey") recordKey: String,
        @Param("fieldName") fieldName: String?
    ): Int
}
