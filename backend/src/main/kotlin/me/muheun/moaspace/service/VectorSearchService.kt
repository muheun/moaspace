package me.muheun.moaspace.service

import me.muheun.moaspace.config.VectorProperties
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.mapper.VectorChunkMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VectorSearchService(
    private val vectorProperties: VectorProperties,
    private val vectorEmbeddingService: VectorEmbeddingService,
    private val vectorChunkMapper: VectorChunkMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

        val pgVector = vectorEmbeddingService.generateEmbedding(
            text = request.query,
            isQuery = true  // 검색어는 Query Prefix 사용
        )
        val queryVector = FloatArray(pgVector.toArray().size) { i ->
            pgVector.toArray()[i].toFloat()
        }
        val entity = request.entity ?: throw IllegalArgumentException("entity는 필수입니다")

        val scores = vectorChunkMapper.findByWeightedFieldScore(
            queryVector = queryVector,
            namespace = request.namespace ?: vectorProperties.namespace,
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

}
