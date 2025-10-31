package me.muheun.moaspace.service

import org.springframework.stereotype.Service

// 토큰 기반 텍스트 청킹
@Service
class ChunkingService(
    private val tokenizerService: TokenizerService
) {

    companion object {
        private const val MAX_TOKENS_PER_CHUNK = 512
        private const val TARGET_TOKENS_PER_CHUNK = 256
    }

    /**
     * 텍스트를 토큰 기반으로 청킹
     *
     * 문장 단위로 분할하고 TARGET_TOKENS_PER_CHUNK를 기준으로 그룹화합니다.
     *
     * @param text 분할할 원본 텍스트
     * @return 청크 리스트 (빈 텍스트일 경우 빈 리스트)
     */
    fun chunkText(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val totalTokens = tokenizerService.countTokens(text)

        if (totalTokens <= TARGET_TOKENS_PER_CHUNK) {
            return listOf(text.trim())
        }

        val sentences = splitIntoSentences(text)
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()
        var currentTokenCount = 0

        for (sentence in sentences) {
            val sentenceTokens = tokenizerService.countTokens(sentence)

            if (sentenceTokens > MAX_TOKENS_PER_CHUNK) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                    currentTokenCount = 0
                }

                val truncated = tokenizerService.truncateToTokenLimit(sentence, MAX_TOKENS_PER_CHUNK)
                chunks.add(truncated)
                continue
            }

            if (currentTokenCount + sentenceTokens > TARGET_TOKENS_PER_CHUNK && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk.clear()
                currentTokenCount = 0
            }

            currentChunk.append(sentence).append(" ")
            currentTokenCount += sentenceTokens
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }

    /**
     * 텍스트를 문장 단위로 분할
     */
    private fun splitIntoSentences(text: String): List<String> {
        val sentencePattern = Regex("""[^.!?]*[.!?]+""")
        val matches = sentencePattern.findAll(text)
        val sentences = matches.map { it.value.trim() }.filter { it.isNotBlank() }.toList()

        if (sentences.isEmpty()) {
            return listOf(text.trim())
        }

        val lastMatchEnd = matches.lastOrNull()?.range?.last ?: -1
        if (lastMatchEnd < text.length - 1) {
            val remaining = text.substring(lastMatchEnd + 1).trim()
            if (remaining.isNotBlank()) {
                return sentences + remaining
            }
        }

        return sentences
    }
}
