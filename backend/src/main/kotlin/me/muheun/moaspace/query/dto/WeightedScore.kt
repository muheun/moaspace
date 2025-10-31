package me.muheun.moaspace.query.dto

/**
 * 가중치 적용 스코어 DTO
 *
 * VectorChunkCustomRepository.findByWeightedFieldScore() 반환 타입
 * 필드별 가중치를 적용한 벡터 검색 결과
 *
 * @property recordKey 레코드 식별자
 * @property weightedScore 가중치 적용 유사도 스코어 (title*titleWeight + content*contentWeight)
 */
data class WeightedScore(
    val recordKey: String,
    val weightedScore: Double
)
