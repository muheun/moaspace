package com.example.vectorboard.dto

/**
 * 벡터 검색 요청 DTO
 *
 * namespace, entity, fieldName으로 검색 범위를 제한하고,
 * 필드별 가중치를 적용할 수 있습니다.
 *
 * @property query 검색 쿼리 텍스트
 * @property namespace 네임스페이스 필터 (null이면 전체)
 * @property entity 엔티티 타입 필터 (null이면 전체)
 * @property fieldName 필드명 필터 (null이면 전체 필드)
 * @property fieldWeights 필드별 가중치 (예: {"title": 0.6, "content": 0.4})
 * @property limit 반환할 결과 개수 (기본값: 10)
 */
data class VectorSearchRequest(
    val query: String,
    val namespace: String? = "vector_ai",
    val entity: String? = null,
    val fieldName: String? = null,
    val fieldWeights: Map<String, Double>? = null,
    val limit: Int = 10
) {
    init {
        require(query.isNotBlank()) { "query는 비어있을 수 없습니다" }
        require(limit > 0) { "limit은 0보다 커야 합니다" }
        require(limit <= 100) { "limit은 100 이하여야 합니다" }

        // 가중치 검증
        fieldWeights?.let { weights ->
            require(weights.values.all { it >= 0.0 && it <= 1.0 }) {
                "가중치는 0.0 ~ 1.0 사이여야 합니다"
            }
            val sum = weights.values.sum()
            require(sum > 0.0) { "가중치 합계는 0보다 커야 합니다" }
        }
    }

    /**
     * 가중치를 정규화하여 반환 (합계가 1.0이 되도록)
     */
    fun normalizedWeights(): Map<String, Double>? {
        return fieldWeights?.let { weights ->
            val sum = weights.values.sum()
            weights.mapValues { (_, value) -> value / sum }
        }
    }
}
