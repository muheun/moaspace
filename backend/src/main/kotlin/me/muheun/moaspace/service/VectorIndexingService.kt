package me.muheun.moaspace.service

import me.muheun.moaspace.domain.vector.VectorChunk
import me.muheun.moaspace.repository.VectorChunkRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 범용 벡터 인덱싱 서비스
 * vector_configs 설정에 따라 엔티티의 필드를 자동으로 벡터화합니다.
 */
@Service
@Transactional(readOnly = true)
class VectorIndexingService(
    private val vectorConfigService: VectorConfigService,
    private val chunkingService: ChunkingService,
    private val embeddingService: VectorEmbeddingService,
    private val vectorChunkRepository: VectorChunkRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val DEFAULT_NAMESPACE = "vector_ai"
    }

    /**
     * 엔티티 벡터 인덱싱
     *
     * @param entityType 엔티티 타입 (예: "Post", "Product")
     * @param recordKey 레코드 식별자 (원본 테이블의 ID)
     * @param fields 필드명 -> 필드값 맵 (예: {"title": "제목", "content": "내용"})
     * @return 생성된 청크 개수
     */
    @Transactional
    fun indexEntity(
        entityType: String,
        recordKey: String,
        fields: Map<String, String>,
        namespace: String = DEFAULT_NAMESPACE
    ): Int {
        logger.info("벡터 인덱싱 시작: entityType=$entityType, recordKey=$recordKey, namespace=$namespace")

        val enabledConfigs = vectorConfigService.findEnabledConfigsByEntityType(entityType)

        if (enabledConfigs.isEmpty()) {
            logger.warn("활성화된 벡터 설정이 없습니다: entityType=$entityType")
            return 0
        }

        logger.debug("활성화된 설정 ${enabledConfigs.size}개 발견: ${enabledConfigs.map { it.fieldName }}")

        var totalChunks = 0

        for (config in enabledConfigs) {
            val fieldName = config.fieldName
            val fieldValue = fields[fieldName]

            if (fieldValue.isNullOrBlank()) {
                logger.debug("필드값이 비어있어 건너뜁니다: fieldName=$fieldName")
                continue
            }

            val chunks = chunkingService.chunkText(fieldValue)
            logger.debug("필드 '$fieldName' 청킹 완료: ${chunks.size}개 청크 생성")

            chunks.forEachIndexed { index, chunkText ->
                try {
                    val embedding = embeddingService.generateEmbedding(chunkText)

                    val vectorChunk = VectorChunk(
                        namespace = namespace,
                        entity = entityType,
                        recordKey = recordKey,
                        fieldName = fieldName,
                        chunkText = chunkText,
                        chunkVector = embedding,
                        chunkIndex = index,
                        startPosition = index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP),
                        endPosition = (index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP)) + chunkText.length
                    )

                    vectorChunkRepository.saveAndFlush(vectorChunk)
                    totalChunks++
                } catch (e: Exception) {
                    logger.error(
                        "벡터화 실패: entityType=$entityType, recordKey=$recordKey, " +
                        "fieldName=$fieldName, chunkIndex=$index, " +
                        "chunkText='${chunkText.take(50)}...', error=${e.message}",
                        e
                    )
                    throw RuntimeException(
                        "벡터 인덱싱 중 오류 발생: fieldName=$fieldName, chunkIndex=$index",
                        e
                    )
                }
            }

            logger.debug("필드 '$fieldName' 벡터화 완료: ${chunks.size}개 청크 저장")
        }

        logger.info("벡터 인덱싱 완료: entityType=$entityType, recordKey=$recordKey, 총 ${totalChunks}개 청크 생성")
        return totalChunks
    }

    /**
     * 엔티티 재인덱싱
     *
     * 기존 벡터를 삭제하고 새로 인덱싱합니다.
     *
     * @param entityType 엔티티 타입
     * @param recordKey 레코드 식별자
     * @param fields 필드명 -> 필드값 맵
     * @return 생성된 청크 개수
     */
    @Transactional
    fun reindexEntity(
        entityType: String,
        recordKey: String,
        fields: Map<String, String>,
        namespace: String = DEFAULT_NAMESPACE
    ): Int {
        logger.info("재인덱싱 시작: entityType=$entityType, recordKey=$recordKey")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = namespace,
            entity = entityType,
            recordKey = recordKey,
            fieldName = null
        )

        logger.debug("기존 청크 삭제 완료: ${deletedCount}개")

        val createdCount = indexEntity(entityType, recordKey, fields, namespace)

        logger.info("재인덱싱 완료: 삭제=${deletedCount}개, 생성=${createdCount}개")
        return createdCount
    }

    /**
     * 엔티티 벡터 인덱스 삭제
     *
     * @param entityType 엔티티 타입
     * @param recordKey 레코드 식별자
     * @return 삭제된 청크 개수
     */
    @Transactional
    fun deleteEntityIndex(
        entityType: String,
        recordKey: String,
        namespace: String = DEFAULT_NAMESPACE
    ): Int {
        logger.info("벡터 인덱스 삭제 시작: entityType=$entityType, recordKey=$recordKey")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = namespace,
            entity = entityType,
            recordKey = recordKey,
            fieldName = null
        )

        logger.info("벡터 인덱스 삭제 완료: ${deletedCount}개 청크 삭제")
        return deletedCount
    }

    /**
     * 특정 필드만 재인덱싱
     *
     * @param entityType 엔티티 타입
     * @param recordKey 레코드 식별자
     * @param fieldName 필드명
     * @param fieldValue 필드값
     * @return 생성된 청크 개수
     */
    @Transactional
    fun reindexField(
        entityType: String,
        recordKey: String,
        fieldName: String,
        fieldValue: String,
        namespace: String = DEFAULT_NAMESPACE
    ): Int {
        logger.info("필드 재인덱싱 시작: entityType=$entityType, recordKey=$recordKey, fieldName=$fieldName")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = namespace,
            entity = entityType,
            recordKey = recordKey,
            fieldName = fieldName
        )

        logger.debug("기존 필드 청크 삭제 완료: ${deletedCount}개")

        if (fieldValue.isBlank()) {
            logger.debug("필드값이 비어있어 재인덱싱 건너뜀")
            return 0
        }

        val chunks = chunkingService.chunkText(fieldValue)
        logger.debug("필드 '$fieldName' 청킹 완료: ${chunks.size}개 청크 생성")

        var createdCount = 0

        chunks.forEachIndexed { index, chunkText ->
            val embedding = embeddingService.generateEmbedding(chunkText)

            val vectorChunk = VectorChunk(
                namespace = namespace,
                entity = entityType,
                recordKey = recordKey,
                fieldName = fieldName,
                chunkText = chunkText,
                chunkVector = embedding,
                chunkIndex = index,
                startPosition = index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP),
                endPosition = (index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP)) + chunkText.length
            )

            vectorChunkRepository.saveAndFlush(vectorChunk)
            createdCount++
        }

        logger.info("필드 재인덱싱 완료: 삭제=${deletedCount}개, 생성=${createdCount}개")
        return createdCount
    }

    /**
     * 여러 엔티티를 배치로 벡터 인덱싱
     *
     * JDBC batch INSERT를 활용하여 대용량 데이터 처리 성능 최적화
     * (application.yml: hibernate.jdbc.batch_size=50)
     *
     * @param entityType 엔티티 타입
     * @param recordsFields 레코드별 필드 맵 리스트 (recordKey -> fields)
     * @param namespace 네임스페이스
     * @return 생성된 총 청크 개수
     */
    @Transactional
    fun indexEntitiesBatch(
        entityType: String,
        recordsFields: List<Pair<String, Map<String, String>>>,
        namespace: String = DEFAULT_NAMESPACE
    ): Int {
        logger.info("배치 벡터 인덱싱 시작: entityType=$entityType, records=${recordsFields.size}개, namespace=$namespace")

        val enabledConfigs = vectorConfigService.findEnabledConfigsByEntityType(entityType)

        if (enabledConfigs.isEmpty()) {
            logger.warn("활성화된 벡터 설정이 없습니다: entityType=$entityType")
            return 0
        }

        logger.debug("활성화된 설정 ${enabledConfigs.size}개 발견: ${enabledConfigs.map { it.fieldName }}")

        val allChunks = mutableListOf<VectorChunk>()

        // 1. 모든 VectorChunk 엔티티 생성 (배치 INSERT 준비)
        for ((recordKey, fields) in recordsFields) {
            for (config in enabledConfigs) {
                val fieldName = config.fieldName
                val fieldValue = fields[fieldName]

                if (fieldValue.isNullOrBlank()) {
                    continue
                }

                val chunks = chunkingService.chunkText(fieldValue)

                chunks.forEachIndexed { index, chunkText ->
                    try {
                        val embedding = embeddingService.generateEmbedding(chunkText)

                        val vectorChunk = VectorChunk(
                            namespace = namespace,
                            entity = entityType,
                            recordKey = recordKey,
                            fieldName = fieldName,
                            chunkText = chunkText,
                            chunkVector = embedding,
                            chunkIndex = index,
                            startPosition = index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP),
                            endPosition = (index * (ChunkingService.DEFAULT_CHUNK_SIZE - ChunkingService.DEFAULT_OVERLAP)) + chunkText.length
                        )

                        allChunks.add(vectorChunk)
                    } catch (e: Exception) {
                        logger.error(
                            "벡터화 실패: entityType=$entityType, recordKey=$recordKey, " +
                            "fieldName=$fieldName, chunkIndex=$index, " +
                            "chunkText='${chunkText.take(50)}...', error=${e.message}",
                            e
                        )
                        throw RuntimeException(
                            "배치 벡터 인덱싱 중 오류 발생: recordKey=$recordKey, fieldName=$fieldName, chunkIndex=$index",
                            e
                        )
                    }
                }
            }
        }

        // 2. JDBC batch INSERT 실행 (hibernate.jdbc.batch_size=50 활용)
        logger.debug("배치 INSERT 실행: ${allChunks.size}개 청크")
        vectorChunkRepository.saveAll(allChunks)
        vectorChunkRepository.flush()

        logger.info("배치 벡터 인덱싱 완료: entityType=$entityType, records=${recordsFields.size}개, 총 ${allChunks.size}개 청크 생성")
        return allChunks.size
    }
}
