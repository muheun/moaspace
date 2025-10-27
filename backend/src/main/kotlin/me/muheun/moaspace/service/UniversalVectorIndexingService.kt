package me.muheun.moaspace.service

import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.dto.VectorSearchResult
import me.muheun.moaspace.event.VectorIndexingRequestedEvent
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorChunkSearchResult
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture

/**
 * 범용 벡터 인덱싱 서비스
 *
 * namespace/entity/recordKey 패턴으로 다양한 엔티티의 벡터 인덱싱을 통합 관리합니다.
 * 동기 삭제와 비동기 벡터 생성을 조합하여 빠른 응답과 안정성을 보장합니다.
 */
@Service
class UniversalVectorIndexingService(
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorService: VectorEmbeddingService,
    private val eventPublisher: ApplicationEventPublisher
) {

    /**
     * 새로운 엔티티를 벡터 인덱스에 추가
     *
     * 이벤트를 발행하고 즉시 반환하며, 백그라운드에서 벡터 생성이 진행됩니다.
     */
    fun indexEntity(request: VectorIndexRequest): CompletableFuture<Unit> {
        eventPublisher.publishEvent(
            VectorIndexingRequestedEvent(
                namespace = request.namespace,
                entity = request.entity,
                recordKey = request.recordKey,
                fields = request.fields,
                metadata = request.metadata
            )
        )

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * 기존 엔티티를 재인덱싱
     *
     * 기존 청크를 동기 삭제한 후 이벤트를 발행하여 고아 청크 발생을 방지합니다.
     */
    @Transactional
    fun reindexEntity(request: VectorIndexRequest): CompletableFuture<Unit> {
        deleteEntity(request.namespace, request.entity, request.recordKey)
        return indexEntity(request)
    }

    /**
     * 엔티티의 모든 벡터 청크 삭제
     */
    @Transactional
    fun deleteEntity(namespace: String, entity: String, recordKey: String) {
        vectorChunkRepository.deleteByNamespaceAndEntityAndRecordKey(namespace, entity, recordKey)
    }

    /**
     * 벡터 유사도 기반 검색
     *
     * 검색어를 벡터로 변환하여 유사한 청크를 찾습니다.
     * 필드별 가중치가 있으면 가중 평균을 적용합니다.
     */
    @Transactional(readOnly = true)
    fun search(request: VectorSearchRequest): List<VectorSearchResult> {
        val queryVector = vectorService.generateEmbedding(request.query)

        return if (request.fieldWeights != null && request.fieldWeights.isNotEmpty()) {
            searchWithFieldWeights(queryVector.toArray(), request)
        } else {
            searchTopChunksByRecord(queryVector.toArray(), request)
        }
    }

    /**
     * 필드별 가중치를 적용한 검색
     *
     * 각 필드의 최고 점수를 가중 평균하여 레코드 점수를 계산합니다.
     */
    private fun searchWithFieldWeights(
        queryVector: FloatArray,
        request: VectorSearchRequest
    ): List<VectorSearchResult> {
        val normalizedWeights = request.normalizedWeights() ?: emptyMap()
        val fieldNames = normalizedWeights.keys.toList()

        val fieldResults = fieldNames.associateWith { fieldName ->
            vectorChunkRepository.findTopChunksByRecord(
                queryVector = queryVector,
                namespace = request.namespace,
                entity = request.entity,
                fieldName = fieldName,
                limit = request.limit * 2
            )
        }

        val recordScores = mutableMapOf<RecordKey, WeightedScore>()

        fieldResults.forEach { (fieldName, results) ->
            val weight = normalizedWeights[fieldName] ?: 0.0
            results.forEach { result ->
                val key = RecordKey(result.getNamespace(), result.getEntity(), result.getRecordKey())
                val current = recordScores[key] ?: WeightedScore(0.0, mutableMapOf())
                current.totalScore += result.getScore() * weight
                current.fieldScores[fieldName] = result.getScore()
                recordScores[key] = current
            }
        }

        val topRecords = recordScores.entries
            .sortedByDescending { it.value.totalScore }
            .take(request.limit)

        return topRecords.mapNotNull { (recordKey, weightedScore) ->
            val bestField = weightedScore.fieldScores.maxByOrNull { it.value }?.key ?: return@mapNotNull null
            val bestFieldResults = fieldResults[bestField] ?: return@mapNotNull null

            val chunkResult = bestFieldResults.firstOrNull {
                it.getNamespace() == recordKey.namespace &&
                it.getEntity() == recordKey.entity &&
                it.getRecordKey() == recordKey.recordKey
            } ?: return@mapNotNull null

            mapChunkToSearchResult(chunkResult, weightedScore.totalScore)
        }
    }

    /**
     * 일반 검색 (필드별 가중치 없음)
     */
    private fun searchTopChunksByRecord(
        queryVector: FloatArray,
        request: VectorSearchRequest
    ): List<VectorSearchResult> {
        val chunkResults = vectorChunkRepository.findTopChunksByRecord(
            queryVector = queryVector,
            namespace = request.namespace,
            entity = request.entity,
            fieldName = request.fieldName,
            limit = request.limit
        )

        return chunkResults.mapNotNull { result ->
            mapChunkToSearchResult(result, result.getScore())
        }
    }

    /**
     * VectorChunkSearchResult를 VectorSearchResult로 변환
     *
     * 중복 코드 제거를 위한 공통 매핑 메서드입니다.
     */
    private fun mapChunkToSearchResult(
        chunkResult: VectorChunkSearchResult,
        similarityScore: Double
    ): VectorSearchResult? {
        val chunk = vectorChunkRepository.findById(chunkResult.getChunkId()).orElse(null)
            ?: return null

        return VectorSearchResult(
            chunkId = chunkResult.getChunkId(),
            namespace = chunkResult.getNamespace(),
            entity = chunkResult.getEntity(),
            recordKey = chunkResult.getRecordKey(),
            fieldName = chunkResult.getFieldName(),
            chunkText = chunk.chunkText,
            chunkIndex = chunk.chunkIndex,
            startPosition = chunk.startPosition,
            endPosition = chunk.endPosition,
            similarityScore = similarityScore,
            metadata = mapOf(
                "namespace" to chunk.namespace,
                "entity" to chunk.entity,
                "field_name" to chunk.fieldName
            ) + (chunk.metadata ?: emptyMap())
        )
    }
}

private data class RecordKey(
    val namespace: String,
    val entity: String,
    val recordKey: String
)

private data class WeightedScore(
    var totalScore: Double,
    val fieldScores: MutableMap<String, Double>
)
