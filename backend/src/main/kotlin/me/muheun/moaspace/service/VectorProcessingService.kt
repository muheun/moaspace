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
 * 모든 엔티티(Post, Product, Comment 등)의 벡터 생성 및 청크 저장을 백그라운드에서 처리합니다.
 * VectorIndexingRequestedEvent를 수신하여 비동기로 VectorChunk를 생성합니다.
 */
@Service
class VectorProcessingService(
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorService: VectorService,
    private val fixedSizeChunkingService: FixedSizeChunkingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 벡터 인덱싱 요청 이벤트 리스너
     *
     * VectorIndexingRequestedEvent를 수신하여 백그라운드에서 벡터 생성 및 저장을 처리합니다.
     *
     * 처리 과정:
     * 1. 각 필드별로 텍스트 청킹
     * 2. 병렬로 벡터 생성
     * 3. VectorChunk 엔티티로 저장
     *
     * @param event 벡터 인덱싱 요청 이벤트
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleVectorIndexingRequest(event: VectorIndexingRequestedEvent) {
        try {
            logger.info("🔵 [범용 인덱싱] 이벤트 수신: namespace=${event.namespace}, entity=${event.entity}, recordKey=${event.recordKey}, fields=${event.fields.keys}")

            // 각 필드별로 처리
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
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 식별자
     * @param fieldName 필드명
     * @param fieldValue 필드 값 (텍스트)
     * @param metadata 추가 메타데이터
     */
    private fun processFieldVectorization(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String,
        fieldValue: String,
        metadata: Map<String, Any>?
    ) {
        try {
            logger.debug("🟡 [필드 벡터화] 시작: $entity.$fieldName (recordKey=$recordKey, 텍스트 길이=${fieldValue.length})")

            // 1. 텍스트 청킹 (토큰 기반 문장 경계 청킹)
            val chunks = fixedSizeChunkingService.chunk(fieldValue)
            logger.debug("🟢 [청킹 완료] $entity.$fieldName: ${chunks.size}개 청크 생성 (토큰 기반)")

            // 2. 병렬로 벡터 생성
            val vectorChunks = runBlocking {
                chunks.map { chunk ->
                    async(Dispatchers.IO) {
                        val vector = vectorService.generateEmbedding(chunk.text)

                        VectorChunk(
                            namespace = namespace,
                            entity = entity,
                            recordKey = recordKey,
                            fieldName = fieldName,
                            chunkText = chunk.text,
                            chunkVector = vector,
                            chunkIndex = chunk.chunkIndex,
                            startPosition = chunk.startPosition,
                            endPosition = chunk.endPosition,
                            metadata = metadata
                        )
                    }
                }.awaitAll()
            }

            // 3. Batch Insert
            vectorChunkRepository.saveAll(vectorChunks)

            logger.debug("✅ [필드 벡터화] 완료: $entity.$fieldName (${chunks.size}개 청크 DB 저장)")

        } catch (e: Exception) {
            logger.error("❌ [필드 벡터화] 실패: $entity.$fieldName (recordKey=$recordKey), error=${e.message}", e)
            throw e
        }
    }
}
