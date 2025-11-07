package me.muheun.moaspace.service

import me.muheun.moaspace.config.VectorProperties
import me.muheun.moaspace.domain.vector.VectorChunk
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.full.memberProperties

/**
 * 범용 벡터 인덱싱 서비스
 *
 * Constitution Principle I 준수:
 * - VectorConfig DB에서 enabled=true인 필드만 동적으로 조회하여 벡터화
 * - 엔티티 필드 추가 시 코드 수정 불필요 (VectorConfig만 INSERT)
 * - namespace 설정으로 멀티테넌시 지원
 */
@Service
@Transactional(readOnly = true)
class VectorIndexingService(
    private val vectorProperties: VectorProperties,
    private val vectorConfigRepository: VectorConfigRepository,
    private val chunkingService: ChunkingService,
    private val embeddingService: VectorEmbeddingService,
    private val vectorChunkRepository: VectorChunkRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Entity에서 벡터화할 필드 추출 (범용)
     *
     * @param entity 벡터화할 Entity 객체 (Post, User 등)
     * @param entityType VectorConfig의 entity_type ("Post", "User")
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 필드명 -> 필드값 맵
     */
    fun extractVectorFields(
        entity: Any,
        entityType: String,
        namespace: String? = null
    ): Map<String, String> {
        val ns = namespace ?: vectorProperties.namespace

        val enabledConfigs = vectorConfigRepository
            .findByNamespaceAndEntityTypeAndEnabled(
                namespace = ns,
                entityType = entityType,
                enabled = true
            )

        if (enabledConfigs.isEmpty()) {
            logger.warn("활성화된 벡터 설정이 없습니다: entityType=$entityType, namespace=$ns")
            return emptyMap()
        }

        logger.debug("활성화된 필드 ${enabledConfigs.size}개: ${enabledConfigs.map { it.fieldName }}")

        return enabledConfigs.mapNotNull { config ->
            val fieldName = config.fieldName

            try {
                val property = entity::class.memberProperties
                    .find { it.name == fieldName }

                if (property == null) {
                    logger.warn("$entityType 에 존재하지 않는 필드: $fieldName")
                    return@mapNotNull null
                }

                val value = property.call(entity)

                val fieldValue = when (value) {
                    is String -> if (value.isNotBlank()) value else null
                    is Array<*> -> {
                        val arrayStr = value.joinToString(" ")
                        if (arrayStr.isNotBlank()) arrayStr else null
                    }
                    is List<*> -> {
                        val listStr = value.joinToString(" ")
                        if (listStr.isNotBlank()) listStr else null
                    }
                    else -> value?.toString()?.takeIf { it.isNotBlank() }
                }

                if (fieldValue.isNullOrBlank()) {
                    logger.debug("필드값이 비어있어 건너뜁니다: $fieldName")
                    null
                } else {
                    fieldName to fieldValue
                }

            } catch (e: Exception) {
                logger.error("필드 추출 실패: entityType=$entityType, fieldName=$fieldName, error=${e.message}", e)
                null
            }
        }.toMap()
    }

    /**
     * 엔티티 벡터 인덱싱
     *
     * @param entityType 엔티티 타입 (예: "Post", "Product")
     * @param recordKey 레코드 식별자 (원본 테이블의 ID)
     * @param fields 필드명 -> 필드값 맵 (예: {"title": "제목", "content_text": "내용"})
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 생성된 청크 개수
     */
    @Transactional
    fun indexEntity(
        entityType: String,
        recordKey: String,
        fields: Map<String, String>,
        namespace: String? = null
    ): Int {
        val ns = namespace ?: vectorProperties.namespace
        logger.info("벡터 인덱싱 시작: entityType=$entityType, recordKey=$recordKey, namespace=$ns")

        val enabledConfigs = vectorConfigRepository.findByNamespaceAndEntityTypeAndEnabled(
            namespace = ns,
            entityType = entityType,
            enabled = true
        )

        if (enabledConfigs.isEmpty()) {
            logger.warn("활성화된 벡터 설정이 없습니다: entityType=$entityType")
            return 0
        }

        logger.debug("활성화된 설정 ${enabledConfigs.size}개 발견: ${enabledConfigs.map { config -> config.fieldName }}")

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
                        namespace = ns,
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
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 생성된 청크 개수
     */
    @Transactional
    fun reindexEntity(
        entityType: String,
        recordKey: String,
        fields: Map<String, String>,
        namespace: String? = null
    ): Int {
        val ns = namespace ?: vectorProperties.namespace
        logger.info("재인덱싱 시작: entityType=$entityType, recordKey=$recordKey, namespace=$ns")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = ns,
            entity = entityType,
            recordKey = recordKey,
            fieldName = null
        )

        logger.debug("기존 청크 삭제 완료: ${deletedCount}개")

        val createdCount = indexEntity(entityType, recordKey, fields, ns)

        logger.info("재인덱싱 완료: 삭제=${deletedCount}개, 생성=${createdCount}개")
        return createdCount
    }

    /**
     * 엔티티 벡터 인덱스 삭제
     *
     * @param entityType 엔티티 타입
     * @param recordKey 레코드 식별자
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 삭제된 청크 개수
     */
    @Transactional
    fun deleteEntityIndex(
        entityType: String,
        recordKey: String,
        namespace: String? = null
    ): Int {
        val ns = namespace ?: vectorProperties.namespace
        logger.info("벡터 인덱스 삭제 시작: entityType=$entityType, recordKey=$recordKey, namespace=$ns")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = ns,
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
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 생성된 청크 개수
     */
    @Transactional
    fun reindexField(
        entityType: String,
        recordKey: String,
        fieldName: String,
        fieldValue: String,
        namespace: String? = null
    ): Int {
        val ns = namespace ?: vectorProperties.namespace
        logger.info("필드 재인덱싱 시작: entityType=$entityType, recordKey=$recordKey, fieldName=$fieldName, namespace=$ns")

        val deletedCount = vectorChunkRepository.deleteByFilters(
            namespace = ns,
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
                namespace = ns,
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
     * @param namespace 네임스페이스 (기본값: vectorProperties.namespace)
     * @return 생성된 총 청크 개수
     */
    @Transactional
    fun indexEntitiesBatch(
        entityType: String,
        recordsFields: List<Pair<String, Map<String, String>>>,
        namespace: String? = null
    ): Int {
        val ns = namespace ?: vectorProperties.namespace
        logger.info("배치 벡터 인덱싱 시작: entityType=$entityType, records=${recordsFields.size}개, namespace=$ns")

        val enabledConfigs = vectorConfigRepository.findByNamespaceAndEntityTypeAndEnabled(
            namespace = ns,
            entityType = entityType,
            enabled = true
        )

        if (enabledConfigs.isEmpty()) {
            logger.warn("활성화된 벡터 설정이 없습니다: entityType=$entityType")
            return 0
        }

        logger.debug("활성화된 설정 ${enabledConfigs.size}개 발견: ${enabledConfigs.map { config -> config.fieldName }}")

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
                            namespace = ns,
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
