package me.muheun.moaspace.repository.impl

import com.querydsl.jpa.impl.JPAQueryFactory
import me.muheun.moaspace.domain.vector.QVectorConfig
import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.query.whereAnd
import me.muheun.moaspace.repository.VectorConfigCustomRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class VectorConfigCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : VectorConfigCustomRepository {

    companion object {
        private val logger = LoggerFactory.getLogger(VectorConfigCustomRepositoryImpl::class.java)
    }

    /**
     * 동적 필터링 통합 조회
     *
     * 기존 메서드 통합:
     * - findByEntityType(entityType)
     * - findByEntityTypeAndFieldName(entityType, fieldName)
     * - findByEnabled(enabled)
     * - findByEntityTypeAndEnabled(entityType, enabled)
     *
     * QueryDSL 동적 조건 조합:
     * - whereAnd() 패턴으로 null이 아닌 조건만 AND로 결합
     * - nullable 파라미터는 검색 조건에서 제외
     *
     * @param entityType 엔티티 타입 (nullable)
     * @param fieldName 필드명 (nullable)
     * @param enabled 활성화 여부 (nullable)
     * @return 필터링된 VectorConfig 목록
     */
    override fun findByFilters(
        entityType: String?,
        fieldName: String?,
        enabled: Boolean?
    ): List<VectorConfig> {
        logger.debug("findByFilters 호출: entityType={}, fieldName={}, enabled={}",
            entityType, fieldName, enabled)

        val qVectorConfig = QVectorConfig.vectorConfig

        val results = queryFactory
            .selectFrom(qVectorConfig)
            .where(
                whereAnd(
                    entityType?.let { qVectorConfig.entityType.eq(it) },
                    fieldName?.let { qVectorConfig.fieldName.eq(it) },
                    enabled?.let { qVectorConfig.enabled.eq(it) }
                )
            )
            .fetch()

        logger.info("findByFilters 완료: 조회 결과 {}건", results.size)

        return results
    }
}
