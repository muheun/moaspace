package me.muheun.moaspace.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * VectorConfig 생성 요청 DTO
 *
 * @property entityType 엔티티 타입 (예: Post, Comment)
 * @property fieldName 벡터화 대상 필드명 (예: title, content)
 * @property weight 검색 가중치 (0.1 ~ 10.0, 기본값 1.0)
 * @property threshold 유사도 스코어 최소 임계값 (0.0 ~ 1.0, 기본값 0.0)
 * @property enabled 활성화 여부 (기본값 true)
 */
data class VectorConfigCreateRequest(
    @field:NotBlank(message = "엔티티 타입은 필수입니다")
    @field:Size(max = 100, message = "엔티티 타입은 100자 이내여야 합니다")
    val entityType: String,

    @field:NotBlank(message = "필드명은 필수입니다")
    @field:Size(max = 100, message = "필드명은 100자 이내여야 합니다")
    val fieldName: String,

    @field:DecimalMin(value = "0.1", message = "가중치는 0.1 이상이어야 합니다")
    @field:DecimalMax(value = "10.0", message = "가중치는 10.0 이하여야 합니다")
    val weight: Double = 1.0,

    @field:DecimalMin(value = "0.0", message = "임계값은 0.0 이상이어야 합니다")
    @field:DecimalMax(value = "1.0", message = "임계값은 1.0 이하여야 합니다")
    val threshold: Double = 0.0,

    val enabled: Boolean = true
)
