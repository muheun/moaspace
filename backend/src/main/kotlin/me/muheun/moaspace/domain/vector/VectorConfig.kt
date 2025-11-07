package me.muheun.moaspace.domain.vector

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 벡터화 설정 엔티티
 *
 * 엔티티별 필드의 벡터 검색 가중치 및 임계값을 저장합니다.
 * (namespace, entity_type, field_name) 복합 유니크 키로 중복 방지.
 *
 * namespace: 멀티테넌시 지원을 위한 격리 단위 (기본값: 'moaspace')
 */
@Entity
@Table(
    name = "vector_configs",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_vector_configs_namespace_entity_field",
            columnNames = ["namespace", "entity_type", "field_name"]
        )
    ],
    indexes = [
        Index(name = "idx_configs_namespace_entity_enabled", columnList = "namespace, entity_type, enabled")
    ]
)
class VectorConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    val namespace: String = "moaspace",

    @Column(name = "entity_type", nullable = false, length = 100)
    val entityType: String,

    @Column(name = "field_name", nullable = false, length = 100)
    val fieldName: String,

    @Column(nullable = false)
    var weight: Double = 1.0,

    @Column(nullable = false)
    var threshold: Double = 0.0,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun toString(): String {
        return "VectorConfig(id=$id, namespace='$namespace', entityType='$entityType', fieldName='$fieldName', weight=$weight, threshold=$threshold, enabled=$enabled)"
    }
}
