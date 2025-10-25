package me.muheun.moaspace.service

import me.muheun.moaspace.dto.ChunkingParams
import me.muheun.moaspace.dto.TextChunk
import org.springframework.stereotype.Service

/**
 * 고정 크기 청킹 서비스 (문장 경계 기반)
 *
 * 텍스트를 토큰 기반으로 고정 크기 청크로 분할하며, 다음 특징을 가집니다:
 *
 * 1. **토큰 기반 크기 계산**: OpenAI API의 토큰 제한에 맞춰 정확한 청크 크기 보장
 * 2. **문장 경계 보존**: 한국어/영어 문장 부호에서만 분할하여 의미 단위 유지
 * 3. **오버랩 처리**: 청크 간 겹침으로 문맥 연속성 보장
 * 4. **짧은 텍스트 최적화**: 임계값 이하 텍스트는 단일 청크로 처리
 *
 * @property tokenizerService OpenAI 토크나이저
 */
@Service
class FixedSizeChunkingService(
    private val tokenizerService: TokenizerService
) {

    /**
     * 텍스트를 고정 크기 청크로 분할
     *
     * @param text 분할할 텍스트
     * @param params 청킹 파라미터 (크기, 오버랩, 임계값)
     * @return 청크 리스트
     */
    fun chunk(text: String, params: ChunkingParams = ChunkingParams()): List<TextChunk> {
        if (text.isBlank()) {
            return emptyList()
        }

        val totalTokens = tokenizerService.countTokens(text)

        // 짧은 텍스트는 단일 청크로 처리
        if (totalTokens <= params.minTokens) {
            return listOf(
                TextChunk(
                    chunkIndex = 0,
                    text = text.trim(),
                    startPosition = 0,
                    endPosition = text.length,
                    tokenCount = totalTokens
                )
            )
        }

        // 문장 단위로 분할
        val sentences = splitIntoSentences(text)

        return chunkBySentences(sentences, params)
    }

    /**
     * 텍스트를 문장 단위로 분할
     *
     * 한국어/영어 문장 부호(. ! ?)를 기준으로 분할하며,
     * 공백과 개행을 정리합니다.
     *
     * @param text 분할할 텍스트
     * @return 문장 리스트
     */
    private fun splitIntoSentences(text: String): List<String> {
        // 한국어/영어 문장 부호를 기준으로 분할 (부호 포함)
        val sentencePattern = Regex("""[^.!?]*[.!?]+""")
        val matches = sentencePattern.findAll(text)

        val sentences = matches.map { it.value.trim() }.filter { it.isNotBlank() }.toList()

        // 매칭되지 않은 나머지 텍스트 처리
        if (sentences.isEmpty()) {
            // 문장 부호가 없는 경우 전체를 하나의 문장으로 처리
            return listOf(text.trim())
        }

        // 마지막 매칭 이후 남은 텍스트 확인
        val lastMatchEnd = matches.lastOrNull()?.range?.last ?: -1
        if (lastMatchEnd < text.length - 1) {
            val remaining = text.substring(lastMatchEnd + 1).trim()
            if (remaining.isNotBlank()) {
                return sentences + remaining
            }
        }

        return sentences
    }

    /**
     * 문장들을 토큰 수 기반으로 청크로 그룹화
     *
     * @param sentences 문장 리스트
     * @param params 청킹 파라미터
     * @return 청크 리스트
     */
    private fun chunkBySentences(sentences: List<String>, params: ChunkingParams): List<TextChunk> {
        val chunks = mutableListOf<TextChunk>()
        val currentChunk = mutableListOf<String>()
        var currentTokenCount = 0
        var chunkStartIndex = 0

        sentences.forEach { sentence ->
            val sentenceTokens = tokenizerService.countTokens(sentence)

            // 현재 청크에 문장을 추가했을 때 최대 크기를 초과하는지 확인
            if (currentTokenCount + sentenceTokens > params.targetTokens && currentChunk.isNotEmpty()) {
                // 현재 청크를 완성하고 저장
                val chunkText = currentChunk.joinToString(" ")
                chunks.add(
                    TextChunk(
                        chunkIndex = chunks.size,
                        text = chunkText,
                        startPosition = chunkStartIndex,
                        endPosition = chunkStartIndex + chunkText.length,
                        tokenCount = currentTokenCount
                    )
                )

                // 오버랩 처리: 마지막 몇 문장을 다음 청크로 이월
                val overlapSentences = calculateOverlap(currentChunk, params.overlap)
                currentChunk.clear()
                currentChunk.addAll(overlapSentences)
                currentTokenCount = overlapSentences.sumOf { tokenizerService.countTokens(it) }

                chunkStartIndex += chunkText.length - overlapSentences.joinToString(" ").length
            }

            // 문장 추가
            currentChunk.add(sentence)
            currentTokenCount += sentenceTokens
        }

        // 남은 청크 처리
        if (currentChunk.isNotEmpty()) {
            val chunkText = currentChunk.joinToString(" ")
            chunks.add(
                TextChunk(
                    chunkIndex = chunks.size,
                    text = chunkText,
                    startPosition = chunkStartIndex,
                    endPosition = chunkStartIndex + chunkText.length,
                    tokenCount = currentTokenCount
                )
            )
        }

        return chunks
    }

    /**
     * 오버랩을 위해 마지막 몇 문장을 선택
     *
     * @param sentences 현재 청크의 문장 리스트
     * @param overlapTokens 오버랩 토큰 수
     * @return 오버랩할 문장 리스트
     */
    private fun calculateOverlap(sentences: List<String>, overlapTokens: Int): List<String> {
        if (overlapTokens == 0 || sentences.isEmpty()) {
            return emptyList()
        }

        val overlapSentences = mutableListOf<String>()
        var accumulatedTokens = 0

        // 뒤에서부터 문장을 선택하여 오버랩 토큰 수에 맞춤
        for (i in sentences.size - 1 downTo 0) {
            val sentence = sentences[i]
            val sentenceTokens = tokenizerService.countTokens(sentence)

            if (accumulatedTokens + sentenceTokens > overlapTokens) {
                break
            }

            overlapSentences.add(0, sentence)
            accumulatedTokens += sentenceTokens
        }

        return overlapSentences
    }
}
