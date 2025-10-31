package me.muheun.moaspace.query.dto

// 필드별 가중치 적용 스코어
data class WeightedScore(
    val recordKey: String,
    val fieldName: String,
    val weightedScore: Double
)
