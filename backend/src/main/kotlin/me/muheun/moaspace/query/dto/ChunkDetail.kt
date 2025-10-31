package me.muheun.moaspace.query.dto

// 청크 상세 정보
data class ChunkDetail(
    val chunkId: Long,
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val fieldName: String,
    val score: Double
)
