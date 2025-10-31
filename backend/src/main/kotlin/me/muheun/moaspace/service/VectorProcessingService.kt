package me.muheun.moaspace.service

import me.muheun.moaspace.domain.VectorChunk
import me.muheun.moaspace.event.VectorIndexingRequestedEvent
import me.muheun.moaspace.repository.VectorChunkRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException

// 범용 벡터 처리 서비스
@Service
open class VectorProcessingService(
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorService: VectorEmbeddingService,
    private val chunkingService: ChunkingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 벡터 인덱싱 요청 이벤트 리스너
     *
     * 각 필드별로 텍스트를 청킹하고 병렬로 벡터를 생성한 후 DB에 저장합니다.
     * 트랜잭션은 processFieldVectorization 내부의 batch save에서만 적용됩니다.
     */
    @EventListener
    @Async
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
     * 벡터 생성은 병렬로 수행하고, DB 저장만 트랜잭션으로 묶습니다.
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

            val chunks = chunkingService.chunkText(fieldValue)
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

            saveVectorChunks(vectorChunks)

            logger.debug("✅ [필드 벡터화] 완료: $entity.$fieldName (${chunks.size}개 청크 DB 저장)")

        } catch (e: Exception) {
            logger.error("❌ [필드 벡터화] 실패: $entity.$fieldName (recordKey=$recordKey), error=${e.message}", e)
            throw e
        }
    }

    /**
     * 벡터 청크를 트랜잭션 단위로 batch 저장
     *
     * Spring @Transactional은 private 메서드에서 작동하지 않으므로 open으로 선언합니다.
     * DB 일시적 실패에 대비하여 최대 3회 재시도합니다 (exponential backoff).
     */
    @Transactional
    @Retryable(
        retryFor = [SQLException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    open fun saveVectorChunks(vectorChunks: List<VectorChunk>) {
        try {
            vectorChunkRepository.saveAll(vectorChunks)
            logger.debug("✓ 벡터 청크 저장 성공 (${vectorChunks.size}개)")
        } catch (e: SQLException) {
            logger.warn("⚠️ 벡터 청크 저장 실패, 재시도 예정: ${e.message}")
            throw e
        }
    }
}
