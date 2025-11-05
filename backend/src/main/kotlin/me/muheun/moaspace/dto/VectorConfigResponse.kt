package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.vector.VectorConfig
import java.time.LocalDateTime

/**
 * VectorConfig 응답 DTO
 *
 * 벡터화 설정 조회 결과를 클라이언트에 반환하기 위한 DTO입니다.
 */
data class VectorConfigResponse(
    val id: Long,
    val entityType: String,
    val fieldName: String,
    val weight: Double,
    val threshold: Double,
    val enabled: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * VectorConfig 엔티티를 VectorConfigResponse DTO로 변환
         *
         * @param config VectorConfig 엔티티
         * @return VectorConfigResponse DTO
         */
        fun from(config: VectorConfig): VectorConfigResponse {
            return VectorConfigResponse(
                id = config.id!!,
                entityType = config.entityType,
                fieldName = config.fieldName,
                weight = config.weight,
                threshold = config.threshold,
                enabled = config.enabled,
                createdAt = config.createdAt,
                updatedAt = config.updatedAt
            )
        }
    }
}
