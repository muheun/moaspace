package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * VectorConfig 엔티티 Repository
 *
 * 벡터화 설정 데이터의 CRUD 작업 및 커스텀 조회 메서드를 제공합니다.
 *
 * Phase 4: QueryDSL 통합
 * - VectorConfigCustomRepository 상속 (findByFilters 메서드 제공)
 * - JpaRepository 기본 메서드 유지 (하위 호환성)
 * - findByFilters()로 동적 필터링 가능 (entityType, fieldName, enabled 조합)
 */
@Repository
interface VectorConfigRepository : JpaRepository<VectorConfig, Long>, VectorConfigCustomRepository {

    /**
     * 특정 엔티티 타입의 모든 설정 조회
     *
     * @param entityType 엔티티 타입 (예: "Post", "Comment")
     * @return 해당 엔티티의 필드별 설정 리스트
     */
    fun findByEntityType(entityType: String): List<VectorConfig>

    /**
     * 특정 엔티티 타입과 필드명으로 설정 조회
     *
     * @param entityType 엔티티 타입 (예: "Post")
     * @param fieldName 필드명 (예: "title")
     * @return 해당 설정 (없으면 null)
     */
    fun findByEntityTypeAndFieldName(entityType: String, fieldName: String): VectorConfig?

    /**
     * 활성화 여부로 설정 필터링
     *
     * @param enabled 활성화 여부 (true: 활성, false: 비활성)
     * @return 활성화 상태가 일치하는 설정 리스트
     */
    fun findByEnabled(enabled: Boolean): List<VectorConfig>

    /**
     * 특정 엔티티 타입의 활성화된 설정만 조회
     *
     * @param entityType 엔티티 타입
     * @param enabled 활성화 여부
     * @return 해당 엔티티의 활성화된 설정 리스트
     */
    fun findByEntityTypeAndEnabled(entityType: String, enabled: Boolean): List<VectorConfig>
}
