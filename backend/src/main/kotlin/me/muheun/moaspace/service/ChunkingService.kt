package me.muheun.moaspace.service

import org.springframework.stereotype.Service

/**
 * Sliding Window 방식 텍스트 청킹 서비스
 *
 * 고정 길이 청크(기본 500자)와 오버랩(기본 50자)을 사용하여
 * 텍스트를 분할합니다.
 */
@Service
class ChunkingService {

    companion object {
        const val DEFAULT_CHUNK_SIZE = 500
        const val DEFAULT_OVERLAP = 50
    }

    /**
     * Sliding Window 방식으로 텍스트 청킹
     *
     * @param text 분할할 원본 텍스트
     * @param chunkSize 청크 크기 (기본 500자)
     * @param overlap 오버랩 크기 (기본 50자)
     * @return 청크 리스트 (빈 텍스트일 경우 빈 리스트)
     */
    fun chunkText(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {
        if (text.isBlank()) return emptyList()

        val trimmedText = text.trim()

        if (trimmedText.length <= chunkSize) {
            return listOf(trimmedText)
        }

        val chunks = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < trimmedText.length) {
            val endIndex = minOf(startIndex + chunkSize, trimmedText.length)
            chunks.add(trimmedText.substring(startIndex, endIndex))

            val step = chunkSize - overlap
            startIndex += step
        }

        return chunks
    }
}
