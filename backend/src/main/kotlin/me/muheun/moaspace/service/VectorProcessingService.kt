package me.muheun.moaspace.service

import me.muheun.moaspace.domain.VectorChunk
import me.muheun.moaspace.event.VectorIndexingRequestedEvent
import me.muheun.moaspace.repository.VectorChunkRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 범용 벡터 처리 서비스
 *
 * VectorIndexingRequestedEvent를 수신하여 백그라운드에서 벡터 생성 및 청크 저장을 처리합니다.
 */
@Service
class VectorProcessingService(
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorService: VectorEmbeddingService,
    private val tokenizerService: TokenizerService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_TOKENS_PER_CHUNK = 512
        private const val TARGET_TOKENS_PER_CHUNK = 256
    }

    /**
     * 벡터 인덱싱 요청 이벤트 리스너
     *
     * 각 필드별로 텍스트를 청킹하고 병렬로 벡터를 생성한 후 DB에 저장합니다.
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleVectorIndexingRequest(event: VectorIndexingRequestedEvent) {
        try {
            logger.info("🔵 [범용 인덱싱] 이벤트 수신: namespace=${event.namespace}, entity=${event.entity}, recordKey=${event.recordKey}, fields=${event.fields.keys}")

            event.fields.forEach { (fieldName, fieldValue) ->
                processFieldVectorization(
                    namespace = event.namespace,
                    entity = event.entity,
                    recordKey = event.recordKey,
                    fieldName = fieldName,
                    fieldValue = fieldValue,
                    metadata = event.metadata
                )
            }

            logger.info("✅ [범용 인덱싱] 완료: namespace=${event.namespace}, entity=${event.entity}, recordKey=${event.recordKey}")

        } catch (e: Exception) {
            logger.error("❌ [범용 인덱싱] 실패: namespace=${event.namespace}, entity=${event.entity}, recordKey=${event.recordKey}, error=${e.message}", e)
        }
    }

    /**
     * 개별 필드의 벡터화 처리
     *
     * 테스트에서 직접 호출할 수 있도록 internal로 공개됩니다.
     */
    internal fun processFieldVectorization(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String,
        fieldValue: String,
        metadata: Map<String, Any>?
    ) {
        try {
            logger.debug("🟡 [필드 벡터화] 시작: $entity.$fieldName (recordKey=$recordKey, 텍스트 길이=${fieldValue.length})")

            val chunks = chunkText(fieldValue)
            logger.debug("🟢 [청킹 완료] $entity.$fieldName: ${chunks.size}개 청크 생성")

            val vectorChunks = runBlocking {
                chunks.mapIndexed { index, chunkText ->
                    async(Dispatchers.IO) {
                        val vector = vectorService.generateEmbedding(chunkText)

                        VectorChunk(
                            namespace = namespace,
                            entity = entity,
                            recordKey = recordKey,
                            fieldName = fieldName,
                            chunkText = chunkText,
                            chunkVector = vector,
                            chunkIndex = index,
                            startPosition = 0,
                            endPosition = chunkText.length,
                            metadata = metadata
                        )
                    }
                }.awaitAll()
            }

            vectorChunkRepository.saveAll(vectorChunks)

            logger.debug("✅ [필드 벡터화] 완료: $entity.$fieldName (${chunks.size}개 청크 DB 저장)")

        } catch (e: Exception) {
            logger.error("❌ [필드 벡터화] 실패: $entity.$fieldName (recordKey=$recordKey), error=${e.message}", e)
            throw e
        }
    }

    /**
     * 텍스트를 토큰 기반으로 청킹
     *
     * 문장 단위로 분할하고 TARGET_TOKENS_PER_CHUNK를 기준으로 그룹화합니다.
     */
    private fun chunkText(text: String): List<String> {
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
