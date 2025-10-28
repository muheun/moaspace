package me.muheun.moaspace.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 벡터화 설정 엔티티
 *
 * 엔티티별 필드의 벡터 검색 가중치 및 임계값을 저장합니다.
 * (entity_type, field_name) 복합 유니크 키로 중복 방지.
 */
@Entity
@Table(
    name = "vector_config",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_entity_field",
            columnNames = ["entity_type", "field_name"]
        )
    ],
    indexes = [
        Index(name = "idx_vector_config_entity_type", columnList = "entity_type"),
        Index(name = "idx_vector_config_enabled", columnList = "enabled")
    ]
)
class VectorConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

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
        return "VectorConfig(id=$id, entityType='$entityType', fieldName='$fieldName', weight=$weight, threshold=$threshold, enabled=$enabled)"
    }
}
