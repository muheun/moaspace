package me.muheun.moaspace.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PostSearchRequest(
    val query: String, // 검색 쿼리 (1자 이상)

    @field:Min(0) @field:Max(1)
    val threshold: Double = 0.6, // 유사도 임계값 (0.0~1.0, 기본값 0.6)

    @field:Min(1) @field:Max(100)
    val limit: Int = 20 // 최대 결과 수 (1~100, 기본값 20)
) {
    init {
        require(query.isNotBlank()) { "검색 쿼리는 비어있을 수 없습니다" }
    }
}
