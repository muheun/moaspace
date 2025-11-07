package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.vector.VectorConfig

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
