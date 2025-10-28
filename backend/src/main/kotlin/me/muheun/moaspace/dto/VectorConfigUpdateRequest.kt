package me.muheun.moaspace.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

/**
 * VectorConfig 수정 요청 DTO
 *
 * 기존 VectorConfig의 일부 필드만 업데이트 가능
 * - entityType, fieldName은 변경 불가 (유니크 키이므로)
 * - weight, threshold, enabled만 수정 허용
 */
data class VectorConfigUpdateRequest(
    @field:DecimalMin(value = "0.1", message = "가중치는 0.1 이상이어야 합니다")
    @field:DecimalMax(value = "10.0", message = "가중치는 10.0 이하여야 합니다")
    val weight: Double? = null,

    @field:DecimalMin(value = "0.0", message = "임계값은 0.0 이상이어야 합니다")
    @field:DecimalMax(value = "1.0", message = "임계값은 1.0 이하여야 합니다")
    val threshold: Double? = null,

    val enabled: Boolean? = null
)
