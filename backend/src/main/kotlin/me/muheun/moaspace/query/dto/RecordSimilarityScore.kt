package me.muheun.moaspace.query.dto

/**
 * 레코드별 유사도 스코어 DTO
 *
 * VectorChunkCustomRepository.findSimilarRecords() 반환 타입
 *
 * @property recordKey 레코드 식별자 (예: "post-123")
 * @property score 코사인 유사도 스코어 (0.0~1.0, 1에 가까울수록 유사)
 */
data class RecordSimilarityScore(
    val recordKey: String,
    val score: Double
)
