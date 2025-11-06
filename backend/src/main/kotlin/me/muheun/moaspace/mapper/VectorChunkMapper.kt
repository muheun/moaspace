package me.muheun.moaspace.mapper

import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

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

    
    fun findByWeightedFieldScore(
        @Param("queryVector") queryVector: FloatArray,
        @Param("namespace") namespace: String?,
        @Param("entity") entity: String,
        @Param("limit") limit: Int
    ): List<WeightedScore>

    
    fun deleteByFilters(
        @Param("namespace") namespace: String,
        @Param("entity") entity: String,
        @Param("recordKey") recordKey: String,
        @Param("fieldName") fieldName: String?
    ): Int
}
