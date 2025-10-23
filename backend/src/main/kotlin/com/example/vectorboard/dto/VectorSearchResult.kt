package com.example.vectorboard.dto

/**
 * 벡터 검색 결과 DTO
 *
 * 검색된 청크의 메타데이터와 유사도 점수를 포함합니다.
 *
 * @property chunkId 청크 ID
 * @property namespace 네임스페이스
 * @property entity 엔티티 타입
 * @property recordKey 레코드 식별자
 * @property fieldName 필드명
 * @property chunkText 청크 텍스트
 * @property chunkIndex 청크 순서
 * @property startPosition 원본 텍스트에서의 시작 위치
 * @property endPosition 원본 텍스트에서의 끝 위치
 * @property similarityScore 유사도 점수 (0.0 ~ 1.0, 높을수록 유사)
 * @property metadata 추가 메타데이터
 */
data class VectorSearchResult(
    val chunkId: Long,
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val fieldName: String,
    val chunkText: String,
    val chunkIndex: Int,
    val startPosition: Int,
    val endPosition: Int,
    val similarityScore: Double,
    val metadata: Map<String, Any>? = null
) {
    /**
     * 유사도 점수를 백분율로 반환
     */
    fun scorePercentage(): Double = similarityScore * 100.0

    /**
     * 유사도 등급 (S, A, B, C, D)
     */
    fun scoreGrade(): String = when {
        similarityScore >= 0.9 -> "S"
        similarityScore >= 0.8 -> "A"
        similarityScore >= 0.7 -> "B"
        similarityScore >= 0.6 -> "C"
        else -> "D"
    }
}

/**
 * 레코드별 검색 결과 DTO (청크 정보 없이 레코드 수준)
 *
 * @property namespace 네임스페이스
 * @property entity 엔티티 타입
 * @property recordKey 레코드 식별자
 * @property similarityScore 유사도 점수 (레코드 내 최대값 또는 평균값)
 */
data class RecordSearchResult(
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val similarityScore: Double
)
