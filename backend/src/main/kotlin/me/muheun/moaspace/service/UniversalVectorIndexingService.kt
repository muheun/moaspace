package me.muheun.moaspace.service

import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.dto.VectorSearchResult
import me.muheun.moaspace.event.VectorIndexingRequestedEvent
import me.muheun.moaspace.repository.VectorChunkRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture

/**
 * 범용 벡터 인덱싱 서비스
 *
 * 모든 엔티티(Post, Product, Comment 등)의 벡터 인덱싱을 관리하는 중앙 서비스입니다.
 * - namespace/entity/recordKey 패턴으로 범용성 제공
 * - 동기 삭제 + 비동기 벡터 생성으로 즉시 응답
 * - Event-Driven 아키텍처로 확장성 보장
 *
 * @property vectorChunkRepository 벡터 청크 저장소
 * @property vectorService 벡터 생성 서비스
 * @property eventPublisher 이벤트 발행기 (비동기 처리용)
 */
@Service
class UniversalVectorIndexingService(
    private val vectorChunkRepository: VectorChunkRepository,
    private val vectorService: VectorService,
    private val eventPublisher: ApplicationEventPublisher
) {

    /**
     * 새로운 엔티티를 벡터 인덱스에 추가합니다.
     *
     * 처리 과정:
     * 1. VectorIndexingRequestedEvent 발행
     * 2. 즉시 CompletableFuture 반환 (응답 블로킹 없음)
     * 3. 백그라운드에서 VectorProcessingService가 이벤트 처리
     *
     * @param request 인덱싱 요청 (namespace, entity, recordKey, fields)
     * @return 비동기 완료 Future
     */
    fun indexEntity(request: VectorIndexRequest): CompletableFuture<Unit> {
        // 이벤트 발행 (비동기 처리)
        eventPublisher.publishEvent(
            VectorIndexingRequestedEvent(
                namespace = request.namespace,
                entity = request.entity,
                recordKey = request.recordKey,
                fields = request.fields,
                metadata = request.metadata
            )
        )

        // 즉시 완료된 Future 반환 (실제 벡터 생성은 백그라운드에서 진행)
        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * 기존 엔티티를 재인덱싱합니다.
     *
     * 처리 과정:
     * 1. 기존 청크 동기 삭제 (즉시 삭제)
     * 2. VectorIndexingRequestedEvent 발행
     * 3. 즉시 CompletableFuture 반환
     * 4. 백그라운드에서 새로운 벡터 생성
     *
     * 주의: 삭제는 동기로 처리하여 고아 청크 발생 방지
     *
     * @param request 재인덱싱 요청
     * @return 비동기 완료 Future
     */
    @Transactional
    fun reindexEntity(request: VectorIndexRequest): CompletableFuture<Unit> {
        // 1. 기존 청크 동기 삭제
        deleteEntity(request.namespace, request.entity, request.recordKey)

        // 2. 새로운 인덱싱 이벤트 발행
        return indexEntity(request)
    }

    /**
     * 엔티티의 모든 벡터 청크를 삭제합니다.
     *
     * 동기 삭제로 즉시 처리됩니다.
     *
     * @param namespace 네임스페이스 (예: "vector_ai")
     * @param entity 엔티티 타입 (예: "posts", "products")
     * @param recordKey 레코드 식별자 (예: "123")
     */
    @Transactional
    fun deleteEntity(namespace: String, entity: String, recordKey: String) {
        vectorChunkRepository.deleteByNamespaceAndEntityAndRecordKey(namespace, entity, recordKey)
    }

    /**
     * 벡터 유사도 기반 검색을 수행합니다.
     *
     * 처리 과정:
     * 1. 검색어를 벡터로 변환
     * 2. 필드별 가중치가 있으면 가중치 적용 검색, 없으면 일반 검색
     * 3. VectorSearchResult 리스트 반환
     *
     * @param request 검색 요청 (query, namespace, entity, fieldName, fieldWeights, limit 등)
     * @return 검색 결과 리스트 (유사도 순)
     */
    @Transactional(readOnly = true)
    fun search(request: VectorSearchRequest): List<VectorSearchResult> {
        // 1. 검색어를 벡터로 변환
        val queryVector = vectorService.generateEmbedding(request.query)
        val queryVectorString = vectorService.vectorToString(queryVector)

        // 2. 필드별 가중치가 있으면 가중치 적용 검색
        return if (request.fieldWeights != null && request.fieldWeights.isNotEmpty()) {
            searchWithFieldWeights(queryVectorString, request)
        } else {
            // 일반 검색 (Record별 최고 유사도 청크)
            searchTopChunksByRecord(queryVectorString, request)
        }
    }

    /**
     * 필드별 가중치를 적용한 검색
     *
     * 각 필드의 최고 점수를 가중 평균하여 레코드 점수를 계산합니다.
     *
     * @param queryVector 검색 쿼리 벡터
     * @param request 검색 요청
     * @return 가중 평균 점수로 정렬된 검색 결과
     */
    private fun searchWithFieldWeights(
        queryVector: String,
        request: VectorSearchRequest
    ): List<VectorSearchResult> {
        val normalizedWeights = request.normalizedWeights() ?: emptyMap()
        val fieldNames = normalizedWeights.keys.toList()

        // 각 필드별로 검색 실행
        val fieldResults = fieldNames.associateWith { fieldName ->
            vectorChunkRepository.findTopChunksByRecord(
                queryVector = queryVector,
                namespace = request.namespace,
                entity = request.entity,
                fieldName = fieldName,
                limit = request.limit * 2 // 여유있게 가져옴
            )
        }

        // 레코드별로 가중 평균 점수 계산
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

        // 점수 순으로 정렬하고 상위 limit개 선택
        val topRecords = recordScores.entries
            .sortedByDescending { it.value.totalScore }
            .take(request.limit)

        // 각 레코드의 최고 점수 필드에서 청크 정보 가져오기
        return topRecords.mapNotNull { (recordKey, weightedScore) ->
            // 가장 높은 점수를 가진 필드 찾기
            val bestField = weightedScore.fieldScores.maxByOrNull { it.value }?.key ?: return@mapNotNull null
            val bestFieldResults = fieldResults[bestField] ?: return@mapNotNull null

            // 해당 레코드의 청크 찾기
            val chunkResult = bestFieldResults.firstOrNull {
                it.getNamespace() == recordKey.namespace &&
                it.getEntity() == recordKey.entity &&
                it.getRecordKey() == recordKey.recordKey
            } ?: return@mapNotNull null

            // 실제 VectorChunk 조회
            val chunk = vectorChunkRepository.findById(chunkResult.getChunkId()).orElse(null)
                ?: return@mapNotNull null

            VectorSearchResult(
                chunkId = chunkResult.getChunkId(),
                namespace = chunkResult.getNamespace(),
                entity = chunkResult.getEntity(),
                recordKey = chunkResult.getRecordKey(),
                fieldName = chunkResult.getFieldName(),
                chunkText = chunk.chunkText,
                chunkIndex = chunk.chunkIndex,
                startPosition = chunk.startPosition,
                endPosition = chunk.endPosition,
                similarityScore = weightedScore.totalScore, // 가중 평균 점수 사용
                metadata = (chunk.metadata ?: emptyMap()) + mapOf(
                    "namespace" to chunkResult.getNamespace(),
                    "entity" to chunkResult.getEntity(),
                    "field_name" to chunkResult.getFieldName()
                )
            )
        }
    }

    /**
     * 일반 검색 (필드별 가중치 없음)
     *
     * @param queryVector 검색 쿼리 벡터
     * @param request 검색 요청
     * @return 검색 결과 리스트
     */
    private fun searchTopChunksByRecord(
        queryVector: String,
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
            // 실제 VectorChunk 조회
            val chunk = vectorChunkRepository.findById(result.getChunkId()).orElse(null)
                ?: return@mapNotNull null

            VectorSearchResult(
                chunkId = result.getChunkId(),
                namespace = result.getNamespace(),
                entity = result.getEntity(),
                recordKey = result.getRecordKey(),
                fieldName = result.getFieldName(),
                chunkText = chunk.chunkText,
                chunkIndex = chunk.chunkIndex,
                startPosition = chunk.startPosition,
                endPosition = chunk.endPosition,
                similarityScore = result.getScore(),
                metadata = (chunk.metadata ?: emptyMap()) + mapOf(
                    "namespace" to result.getNamespace(),
                    "entity" to result.getEntity(),
                    "field_name" to result.getFieldName()
                )
            )
        }
    }
}

/**
 * 레코드 키 (namespace + entity + recordKey)
 */
private data class RecordKey(
    val namespace: String,
    val entity: String,
    val recordKey: String
)

/**
 * 가중치 적용 점수
 */
private data class WeightedScore(
    var totalScore: Double,
    val fieldScores: MutableMap<String, Double>
)
