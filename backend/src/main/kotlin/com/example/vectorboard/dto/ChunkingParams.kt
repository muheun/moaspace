package com.example.vectorboard.dto

/**
 * 청킹 파라미터 DTO
 *
 * 텍스트 청킹 시 사용할 파라미터를 정의합니다.
 *
 * @property minTokens 최소 토큰 수 (이보다 작으면 청킹하지 않음)
 * @property targetTokens 목표 토큰 수 (청크 크기 목표값)
 * @property maxTokens 최대 토큰 수 (청크 크기 상한)
 * @property overlap 오버랩 토큰 수 (인접 청크 간 중복 영역)
 * @property preserveSentences 문장 경계 보존 여부
 */
data class ChunkingParams(
    val minTokens: Int = DEFAULT_MIN_TOKENS,
    val targetTokens: Int = DEFAULT_TARGET_TOKENS,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val overlap: Int = DEFAULT_OVERLAP,
    val preserveSentences: Boolean = true
) {
    init {
        require(minTokens > 0) { "minTokens는 0보다 커야 합니다" }
        require(targetTokens >= minTokens) { "targetTokens는 minTokens 이상이어야 합니다" }
        require(maxTokens >= targetTokens) { "maxTokens는 targetTokens 이상이어야 합니다" }
        require(overlap >= 0) { "overlap은 0 이상이어야 합니다" }
        require(overlap < targetTokens) { "overlap은 targetTokens보다 작아야 합니다" }
    }

    companion object {
        /**
         * 기본 최소 토큰 수
         * 이보다 짧은 텍스트는 청킹하지 않고 단일 청크로 처리
         */
        const val DEFAULT_MIN_TOKENS = 100

        /**
         * 기본 목표 토큰 수
         * 대부분의 청크가 이 크기를 목표로 생성됨
         */
        const val DEFAULT_TARGET_TOKENS = 256

        /**
         * 기본 최대 토큰 수
         * 청크 크기의 상한선
         */
        const val DEFAULT_MAX_TOKENS = 512

        /**
         * 기본 오버랩 토큰 수
         * 인접 청크 간 문맥 연속성을 위한 중복 영역
         */
        const val DEFAULT_OVERLAP = 50

        /**
         * 기본 파라미터 인스턴스
         */
        fun default() = ChunkingParams()

        /**
         * 짧은 텍스트용 파라미터 (오버랩 없음)
         */
        fun forShortText() = ChunkingParams(
            minTokens = 50,
            targetTokens = 128,
            maxTokens = 256,
            overlap = 0
        )

        /**
         * 긴 문서용 파라미터 (큰 오버랩)
         */
        fun forLongDocument() = ChunkingParams(
            minTokens = 100,
            targetTokens = 512,
            maxTokens = 1024,
            overlap = 100
        )
    }

    /**
     * 유효 청크 크기 (오버랩 제외)
     */
    fun effectiveChunkSize(): Int = targetTokens - overlap

    /**
     * 파라미터 요약 정보
     */
    override fun toString(): String {
        return "ChunkingParams(min=$minTokens, target=$targetTokens, max=$maxTokens, overlap=$overlap, " +
                "preserveSentences=$preserveSentences)"
    }
}
