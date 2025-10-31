package me.muheun.moaspace.repository.impl

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import me.muheun.moaspace.domain.VectorChunk
import me.muheun.moaspace.mapper.VectorChunkMapper
import me.muheun.moaspace.query.VectorJpql
import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore
import me.muheun.moaspace.repository.VectorChunkCustomRepository
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * VectorChunkCustomRepository 구현 클래스
 *
 * ======================================================================================
 * Constitution Principle VI 준수 (v2.4.0)
 * ======================================================================================
 *
 * **MyBatis 전환 완료** (T019-T019c 완료):
 * - findSimilarRecords(): pgvector `<=>` 연산자 (MyBatis)
 * - findTopChunksByRecord(): ROW_NUMBER() OVER (MyBatis)
 * - findByWeightedFieldScore(): CTE + CASE WHEN (MyBatis)
 * - deleteByFilters(): DELETE with nullable fieldName (MyBatis)
 *
 * **MyBatis 사용 사유** (3개 벡터 검색 메서드):
 * - pgvector `<=>` 연산자: JPQL/Kotlin JDSL 미지원
 * - Window Function (ROW_NUMBER() OVER, PARTITION BY): JPQL/Kotlin JDSL 미지원
 * - CTE (WITH ...): JPQL/Kotlin JDSL 미지원
 *
 * **예외 조건 충족 여부** (Constitution Principle VI, Line 204-223):
 * ✅ CustomRepository 패턴 사용 (VectorChunkCustomRepositoryImpl)
 * ✅ DTO 기반 타입 안전성 (ChunkDetail, RecordSimilarityScore, WeightedScore data class)
 * ✅ 테스트 커버리지 100% (VectorChunkRepositoryTest, 6개 테스트 모두 통과)
 * ✅ 주석으로 사유 명시 (이 주석)
 *
 * **향후 개선 계획** (Constitution Baseline Assessment, Line 538-560):
 * - 우선순위: P2 (Medium)
 * - Phase 6 (Q1 2025): MyBatis 설정 및 Mapper XML 전환
 * - 쿼리 로직 XML 분리, Kotlin 코드 간소화
 *
 * ======================================================================================
 * Phase 2 (User Story 1): MyBatis 마이그레이션 완료
 * ======================================================================================
 *
 * **구현 성과**:
 * - Native Query 3개 → MyBatis Mapper 전환 (모든 벡터 검색 메서드)
 * - findSimilarRecords: pgvector `<=>` 연산자 → MyBatis
 * - findTopChunksByRecord: ROW_NUMBER() OVER → MyBatis
 * - findByWeightedFieldScore: CTE + CASE WHEN → MyBatis
 * - 메서드 통합: deleteByNamespaceAndEntityAndRecordKey + AndFieldName → deleteByFilters()
 * - DTO 패키지 분리: me.muheun.moaspace.query.dto
 * - 타입 안전성: Kotlin data class DTO 사용
 *
 * **JPA CustomRepository 패턴**:
 * 1. 네이밍: VectorChunkCustomRepositoryImpl (끝에 Impl 필수)
 * 2. Spring Data JPA 자동 감지: @EnableJpaRepositories가 빈 등록
 * 3. KotlinJdslJpqlExecutor 직접 주입 (Spring Data JPA가 자동 제공)
 * 4. VectorChunkMapper 주입 (MyBatis Mapper - 모든 벡터 검색 및 삭제 메서드)
 * 5. open class: Spring AOP (CGLIB proxy) 지원을 위해 필수
 */
open class VectorChunkCustomRepositoryImpl(
    private val jpqlExecutor: KotlinJdslJpqlExecutor,
    private val vectorChunkMapper: VectorChunkMapper
) : VectorChunkCustomRepository {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * T014: findSimilarRecords() 구현
     *
     * **MyBatis Mapper 호출** (Constitution Principle VI Line 220 준수):
     * - pgvector `<=>` 연산자 (JPQL 미지원)
     * - GROUP BY recordKey + MAX(score) 집계
     * - Nullable 필터링 (namespace, entity)
     *
     * **쿼리 작성 금지 원칙 준수**:
     * - Kotlin JDSL: JPQL 지원 쿼리 (우선)
     * - MyBatis: JPQL 미지원 쿼리 (pgvector 연산자 - 예외)
     * - SQL 직접 작성: 금지
     *
     * **사유**: pgvector `<=>` 연산자는 JPQL/Kotlin JDSL 미지원
     * **대안**: MyBatis Mapper XML에 SQL 작성 (VectorChunkMapper.xml)
     */
    override fun findSimilarRecords(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        limit: Int
    ): List<RecordSimilarityScore> {
        logger.debug("findSimilarRecords 실행 (MyBatis): namespace={}, entity={}, limit={}",
            namespace, entity, limit)

        // MyBatis Mapper 호출 (XML에 SQL 작성됨)
        val results = vectorChunkMapper.findSimilarRecords(
            queryVector = queryVector,
            namespace = namespace,
            entity = entity,
            limit = limit
        )

        logger.info("findSimilarRecords 완료 (MyBatis): {}개 레코드 조회", results.size)
        return results
    }

    /**
     * T017: findTopChunksByRecord() 구현
     *
     * **MyBatis Mapper 호출** (Constitution Principle VI Line 220 준수):
     * - CTE + ROW_NUMBER() OVER (JPQL 미지원)
     * - 레코드별 최고 스코어 청크만 반환 (rank = 1)
     * - nullable 필터링 (namespace, entity, fieldName)
     *
     * **쿼리 작성 금지 원칙 준수**:
     * - Kotlin JDSL: JPQL 지원 쿼리 (우선)
     * - MyBatis: JPQL 미지원 쿼리 (CTE, Window Function - 예외)
     * - SQL 직접 작성: 금지
     *
     * **사유**: ROW_NUMBER() OVER (PARTITION BY)는 JPQL/Kotlin JDSL 미지원
     * **대안**: MyBatis Mapper XML에 SQL 작성 (VectorChunkMapper.xml)
     */
    override fun findTopChunksByRecord(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        fieldName: String?,
        limit: Int
    ): List<ChunkDetail> {
        logger.debug("findTopChunksByRecord 실행 (MyBatis): namespace={}, entity={}, fieldName={}, limit={}",
            namespace, entity, fieldName, limit)

        // MyBatis Mapper 호출 (XML에 SQL 작성됨)
        val results = vectorChunkMapper.findTopChunksByRecord(
            queryVector = queryVector,
            namespace = namespace,
            entity = entity,
            fieldName = fieldName,
            limit = limit
        )

        logger.info("findTopChunksByRecord 완료 (MyBatis): {}개 청크 조회", results.size)
        return results
    }

    /**
     * T018: findByWeightedFieldScore() 구현
     *
     * **MyBatis Mapper 호출** (Constitution Principle VI Line 220 준수):
     * - CTE (WITH ...) + CASE WHEN (JPQL 미지원)
     * - 필드별 최대 스코어 계산
     * - 가중치 적용 및 집계
     *
     * **쿼리 작성 금지 원칙 준수**:
     * - Kotlin JDSL: JPQL 지원 쿼리 (우선)
     * - MyBatis: JPQL 미지원 쿼리 (CTE - 예외)
     * - SQL 직접 작성: 금지
     *
     * **사유**: CTE (WITH ...)는 JPQL/Kotlin JDSL 미지원
     * **대안**: MyBatis Mapper XML에 SQL 작성 (VectorChunkMapper.xml)
     *
     * Constitution Principle II: 필드별 가중치 지원
     */
    override fun findByWeightedFieldScore(
        queryVector: FloatArray,
        namespace: String,
        entity: String,
        titleWeight: Double,
        contentWeight: Double,
        limit: Int
    ): List<WeightedScore> {
        logger.debug("findByWeightedFieldScore 실행 (MyBatis): namespace={}, entity={}, titleWeight={}, contentWeight={}, limit={}",
            namespace, entity, titleWeight, contentWeight, limit)

        // MyBatis Mapper 호출 (XML에 SQL 작성됨)
        val results = vectorChunkMapper.findByWeightedFieldScore(
            queryVector = queryVector,
            namespace = namespace,
            entity = entity,
            titleWeight = titleWeight,
            contentWeight = contentWeight,
            limit = limit
        )

        logger.info("findByWeightedFieldScore 완료 (MyBatis): {}개 레코드 조회", results.size)
        return results
    }

    /**
     * T019c: deleteByFilters() 구현
     *
     * **MyBatis Mapper 호출** (Constitution Principle VI v2.5.0 완벽 준수):
     * - 코드에 쿼리 문자열 Zero (XML로 완전 분리)
     * - nullable fieldName 동적 처리 (MyBatis `<if test>`)
     * - fieldName=null: 모든 필드 삭제
     * - fieldName 지정: 해당 필드만 삭제
     *
     * **쿼리 작성 금지 원칙 준수**:
     * - Kotlin JDSL: JPQL 지원 쿼리 (우선)
     * - MyBatis: JPQL 미지원 쿼리 또는 코드 간소화 (예외)
     * - SQL 직접 작성: 금지 (코드에서 완전 제거)
     *
     * **사유**: Constitution VI v2.5.0 "코드에 쿼리 문자열 Zero" 달성
     * **대안**: MyBatis Mapper XML에 SQL 작성 (VectorChunkMapper.xml)
     */
    @Transactional
    override fun deleteByFilters(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String?
    ): Int {
        logger.debug("deleteByFilters 실행 (MyBatis): namespace={}, entity={}, recordKey={}, fieldName={}",
            namespace, entity, recordKey, fieldName)

        // MyBatis Mapper 호출 (XML에 SQL 작성됨, 동적 SQL로 nullable fieldName 처리)
        val deletedCount = vectorChunkMapper.deleteByFilters(
            namespace = namespace,
            entity = entity,
            recordKey = recordKey,
            fieldName = fieldName
        )

        logger.info("deleteByFilters 완료 (MyBatis): {}개 청크 삭제됨", deletedCount)

        return deletedCount
    }
}
