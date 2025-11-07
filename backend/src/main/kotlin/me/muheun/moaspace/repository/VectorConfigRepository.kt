package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.vector.VectorConfig
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
 * - findByFilters()로 동적 필터링 가능 (namespace, entityType, fieldName, enabled 조합)
 *
 * Phase 6: Namespace 지원
 * - 모든 조회 메서드에 namespace 파라미터 추가
 * - 멀티테넌시 격리 보장
 */
@Repository
interface VectorConfigRepository : JpaRepository<VectorConfig, Long>, VectorConfigCustomRepository {

    /**
     * 특정 네임스페이스 및 엔티티 타입의 모든 설정 조회
     *
     * @param namespace 네임스페이스 (예: "moaspace")
     * @param entityType 엔티티 타입 (예: "Post", "Comment")
     * @return 해당 엔티티의 필드별 설정 리스트
     */
    fun findByNamespaceAndEntityType(namespace: String, entityType: String): List<VectorConfig>

    /**
     * 특정 네임스페이스, 엔티티 타입과 필드명으로 설정 조회
     *
     * @param namespace 네임스페이스
     * @param entityType 엔티티 타입 (예: "Post")
     * @param fieldName 필드명 (예: "title")
     * @return 해당 설정 (없으면 null)
     */
    fun findByNamespaceAndEntityTypeAndFieldName(
        namespace: String,
        entityType: String,
        fieldName: String
    ): VectorConfig?

    /**
     * 특정 네임스페이스의 활성화된 설정만 조회
     *
     * @param namespace 네임스페이스
     * @param enabled 활성화 여부 (true: 활성, false: 비활성)
     * @return 활성화 상태가 일치하는 설정 리스트
     */
    fun findByNamespaceAndEnabled(namespace: String, enabled: Boolean): List<VectorConfig>

    /**
     * 특정 네임스페이스 및 엔티티 타입의 활성화된 설정만 조회
     *
     * ⭐ VectorIndexingService 핵심 메서드
     * - 동적 필드 추출 시 사용
     * - VectorConfig에서 enabled=true인 필드 목록만 반환
     *
     * @param namespace 네임스페이스
     * @param entityType 엔티티 타입
     * @param enabled 활성화 여부
     * @return 해당 엔티티의 활성화된 설정 리스트
     */
    fun findByNamespaceAndEntityTypeAndEnabled(
        namespace: String,
        entityType: String,
        enabled: Boolean
    ): List<VectorConfig>

    // ===== 하위 호환성 메서드 (Deprecated) =====

    /**
     * @deprecated namespace 파라미터 없는 메서드 (하위 호환성)
     * @see findByNamespaceAndEntityType
     */
    @Deprecated("Use findByNamespaceAndEntityType instead", ReplaceWith("findByNamespaceAndEntityType(namespace, entityType)"))
    fun findByEntityType(entityType: String): List<VectorConfig>

    /**
     * @deprecated namespace 파라미터 없는 메서드 (하위 호환성)
     * @see findByNamespaceAndEntityTypeAndFieldName
     */
    @Deprecated("Use findByNamespaceAndEntityTypeAndFieldName instead", ReplaceWith("findByNamespaceAndEntityTypeAndFieldName(namespace, entityType, fieldName)"))
    fun findByEntityTypeAndFieldName(entityType: String, fieldName: String): VectorConfig?

    /**
     * @deprecated namespace 파라미터 없는 메서드 (하위 호환성)
     * @see findByNamespaceAndEnabled
     */
    @Deprecated("Use findByNamespaceAndEnabled instead", ReplaceWith("findByNamespaceAndEnabled(namespace, enabled)"))
    fun findByEnabled(enabled: Boolean): List<VectorConfig>

    /**
     * @deprecated namespace 파라미터 없는 메서드 (하위 호환성)
     * @see findByNamespaceAndEntityTypeAndEnabled
     */
    @Deprecated("Use findByNamespaceAndEntityTypeAndEnabled instead", ReplaceWith("findByNamespaceAndEntityTypeAndEnabled(namespace, entityType, enabled)"))
    fun findByEntityTypeAndEnabled(entityType: String, enabled: Boolean): List<VectorConfig>
}
