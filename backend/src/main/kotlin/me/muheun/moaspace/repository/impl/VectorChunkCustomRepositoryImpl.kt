package me.muheun.moaspace.repository.impl

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import me.muheun.moaspace.mapper.VectorChunkMapper
import me.muheun.moaspace.query.dto.ChunkDetail
import me.muheun.moaspace.query.dto.RecordSimilarityScore
import me.muheun.moaspace.query.dto.WeightedScore
import me.muheun.moaspace.repository.VectorChunkCustomRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class VectorChunkCustomRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
    private val vectorChunkMapper: VectorChunkMapper,
    private val entityManager: EntityManager
) : VectorChunkCustomRepository {

    companion object {
        private val logger = LoggerFactory.getLogger(VectorChunkCustomRepositoryImpl::class.java)
    }

    override fun findSimilarRecords(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        limit: Int
    ): List<RecordSimilarityScore> {
        logger.debug("findSimilarRecords 호출: vectorSize={}, namespace={}, entity={}, limit={}",
            queryVector.size, namespace, entity, limit)

        val results = vectorChunkMapper.findSimilarRecords(queryVector, namespace, entity, limit)

        logger.info("findSimilarRecords 완료: 검색된 레코드 수={}, 상위 스코어={}",
            results.size, results.firstOrNull()?.score)

        return results
    }

    override fun findTopChunksByRecord(
        queryVector: FloatArray,
        namespace: String?,
        entity: String?,
        fieldName: String?,
        limit: Int
    ): List<ChunkDetail> {
        logger.debug("findTopChunksByRecord 호출: vectorSize={}, namespace={}, entity={}, fieldName={}, limit={}",
            queryVector.size, namespace, entity, fieldName, limit)

        val results = vectorChunkMapper.findTopChunksByRecord(queryVector, namespace, entity, fieldName, limit)

        logger.info("findTopChunksByRecord 완료: 검색된 청크 수={}, 레코드 수={}",
            results.size, results.map { it.recordKey }.distinct().size)

        return results
    }

    override fun findByWeightedFieldScore(
        queryVector: FloatArray,
        namespace: String,
        entity: String,
        limit: Int
    ): List<WeightedScore> {
        logger.debug("findByWeightedFieldScore 호출: vectorSize={}, namespace={}, entity={}, limit={}",
            queryVector.size, namespace, entity, limit)

        val results = vectorChunkMapper.findByWeightedFieldScore(
            queryVector,
            namespace,
            entity,
            limit
        )

        logger.info("findByWeightedFieldScore 완료: 검색된 스코어 수={}, 필드별 분포={}",
            results.size,
            results.groupBy { it.fieldName }.mapValues { it.value.size })

        return results
    }

    override fun deleteByFilters(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String?
    ): Int {
        logger.debug("deleteByFilters 호출: namespace={}, entity={}, recordKey={}, fieldName={}",
            namespace, entity, recordKey, fieldName)

        val deletedCount = vectorChunkMapper.deleteByFilters(namespace, entity, recordKey, fieldName)

        if (deletedCount > 0) {
            logger.info("deleteByFilters 완료: 삭제된 행 수={}", deletedCount)
        } else {
            logger.warn("deleteByFilters 완료: 삭제된 행 없음 - namespace={}, entity={}, recordKey={}, fieldName={}",
                namespace, entity, recordKey, fieldName)
        }

        return deletedCount
    }
}
