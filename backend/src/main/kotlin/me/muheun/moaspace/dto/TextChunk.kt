package me.muheun.moaspace.dto

/**
 * 텍스트 청킹 결과 DTO
 *
 * 텍스트를 청크 단위로 분할한 결과를 나타냅니다.
 *
 * @property chunkIndex 청크 순서 (0부터 시작)
 * @property text 청크 텍스트
 * @property startPosition 원본 텍스트에서의 시작 위치 (문자 단위)
 * @property endPosition 원본 텍스트에서의 끝 위치 (문자 단위)
 * @property tokenCount 토큰 개수 (GPT 토크나이저 기준)
 */
data class TextChunk(
    val chunkIndex: Int,
    val text: String,
    val startPosition: Int,
    val endPosition: Int,
    val tokenCount: Int
) {
    /**
     * 청크 길이 (문자 수)
     */
    fun length(): Int = text.length

    /**
     * 청크 범위를 문자열로 반환
     */
    fun positionRange(): String = "$startPosition-$endPosition"

    /**
     * 디버깅용 요약 정보
     */
    override fun toString(): String {
        return "TextChunk(index=$chunkIndex, tokens=$tokenCount, chars=${length()}, range=${positionRange()})"
    }
}

/**
 * 청킹 결과 전체를 나타내는 DTO
 *
 * @property chunks 청크 리스트
 * @property totalChunks 전체 청크 개수
 * @property originalLength 원본 텍스트 길이
 * @property totalTokens 전체 토큰 개수
 */
data class ChunkingResult(
    val chunks: List<TextChunk>,
    val totalChunks: Int = chunks.size,
    val originalLength: Int,
    val totalTokens: Int = chunks.sumOf { it.tokenCount }
) {
    /**
     * 평균 청크 크기 (토큰 수)
     */
    fun averageChunkSize(): Double {
        return if (totalChunks > 0) totalTokens.toDouble() / totalChunks else 0.0
    }

    /**
     * 청킹 효율 (원본 대비 청크 개수)
     */
    fun chunkingEfficiency(): Double {
        return if (originalLength > 0) totalChunks.toDouble() / originalLength else 0.0
    }
}
