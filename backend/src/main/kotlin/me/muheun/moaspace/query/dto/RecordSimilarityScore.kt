package me.muheun.moaspace.query.dto

// 레코드별 유사도 스코어
data class RecordSimilarityScore(
    val recordKey: String,
    val score: Double
)
