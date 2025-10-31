package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorConfig

/**
 * VectorConfig 엔티티에 대한 Kotlin JDSL 기반 Custom Repository 인터페이스
 *
 * Constitution Principle VI 준수:
 * - JpaRepository 메서드 쿼리 우선
 * - 복잡한 동적 조건은 Kotlin JDSL 사용
 *
 * JPA CustomRepository 패턴:
 * 1. 이 인터페이스: 순수 메서드 시그니처 정의 (계약)
 * 2. VectorConfigCustomRepositoryImpl: 실제 Kotlin JDSL 쿼리 구현
 * 3. VectorConfigRepository: JpaRepository + Custom 통합
 *
 * Phase: 3 (선택적)
 */
interface VectorConfigCustomRepository {

    /**
     * 동적 필터링 통합 조회
     *
     * 기존 메서드들:
     * - findByEntityType(entityType)
     * - findByEntityTypeAndFieldName(entityType, fieldName)
     * - findByEnabled(enabled)
     * - findByEntityTypeAndEnabled(entityType, enabled)
     *
     * Kotlin JDSL 통합:
     * - whereAnd() 패턴으로 동적 조건 조합
     * - nullable 파라미터는 검색 조건에서 제외
     * - 일관성 있는 Repository 구조 유지
     *
     * @param entityType 엔티티 타입 (nullable)
     * @param fieldName 필드명 (nullable)
     * @param enabled 활성화 여부 (nullable)
     * @return 필터링된 VectorConfig 목록
     */
    fun findByFilters(
        entityType: String?,
        fieldName: String?,
        enabled: Boolean?
    ): List<VectorConfig>
}
