package me.muheun.moaspace.service

import me.muheun.moaspace.dto.PostSearchRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.mapper.VectorChunkMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 범용 벡터 검색 서비스 (T026)
 *
 * vector_configs 설정에 따라 필드별 가중치를 적용하여 멀티필드 검색을 수행합니다.
 * MyBatis 쿼리에서 vector_configs JOIN을 통해 동적으로 가중치 및 임계값 적용됩니다.
 *
 * Constitution Principle II: 필드별 벡터화 및 가중치 설정 지원
 * Constitution Principle III: 스코어 임계값 필터링 구현
 *
 * Success Criteria SC-003: title weight=3.0 → content 대비 최소 2배 이상 높은 스코어
 * Success Criteria SC-006: threshold=0.7 → 0.65 제외 (임계값 경계 테스트)
 */
@Service
@Transactional(readOnly = true)
class VectorSearchService(
    private val vectorEmbeddingService: VectorEmbeddingService,
    private val vectorChunkMapper: VectorChunkMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val DEFAULT_NAMESPACE = "vector_ai"
    }

    /**
     * 범용 가중치 검색 (레코드별 최종 스코어 반환)
     *
     * MyBatis 쿼리에서 vector_configs와 JOIN하여 가중치 및 임계값을 적용합니다.
     * Service 계층에서는 단순히 쿼리 호출 및 결과 집계만 수행합니다.
     *
     * @param request 검색 요청 (query, namespace, entity, limit)
     * @return Map<String, Double> (recordKey -> 최종 가중치 스코어)
     */
    fun search(request: VectorSearchRequest): Map<String, Double> {
        logger.info("벡터 검색 시작: entity=${request.entity}, query=${request.query}")

        val pgVector = vectorEmbeddingService.generateEmbedding(request.query)
        val queryVector = FloatArray(pgVector.toArray().size) { i ->
            pgVector.toArray()[i].toFloat()
        }
        val entity = request.entity ?: throw IllegalArgumentException("entity는 필수입니다")

        val scores = vectorChunkMapper.findByWeightedFieldScore(
            queryVector = queryVector,
            namespace = request.namespace ?: DEFAULT_NAMESPACE,
            entity = entity,
            limit = request.limit * 10
        )

        val recordScores = scores.groupBy { it.recordKey }
            .mapValues { (_, fieldScores) ->
                fieldScores.sumOf { it.weightedScore }
            }
            .entries
            .sortedByDescending { it.value }
            .take(request.limit)
            .associate { it.key to it.value }

        logger.info("벡터 검색 완료: ${recordScores.size}개 결과 반환")
        return recordScores
    }

    /**
     * Post 전용 가중치 검색 (PostSearchRequest 지원)
     *
     * @param request PostSearchRequest (query, threshold, limit)
     * @return Map<Long, Double> (postId -> 최종 가중치 스코어)
     */
    fun searchPosts(request: PostSearchRequest): Map<Long, Double> {
        val vectorRequest = VectorSearchRequest(
            query = request.query,
            namespace = DEFAULT_NAMESPACE,
            entity = "Post",
            fieldName = null,
            fieldWeights = null,
            limit = request.limit
        )

        return search(vectorRequest).mapKeys { it.key.toLong() }
    }
}
