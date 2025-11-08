package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.vector.VectorChunk
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VectorChunkRepository :
    JpaRepository<VectorChunk, Long>,
    VectorChunkCustomRepository {

    /**
     * 특정 레코드의 모든 청크 조회 (순서대로)
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     * @return 청크 리스트 (chunkIndex 오름차순)
     */
    fun findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
        namespace: String,
        entity: String,
        recordKey: String
    ): List<VectorChunk>

    /**
     * 특정 레코드의 특정 필드 청크만 조회
     *
     * @param namespace 네임스페이스
     * @param entity 엔티티 타입
     * @param recordKey 레코드 ID
     * @param fieldName 필드명
     * @return 청크 리스트 (chunkIndex 오름차순)
     */
    fun findByNamespaceAndEntityAndRecordKeyAndFieldNameOrderByChunkIndexAsc(
        namespace: String,
        entity: String,
        recordKey: String,
        fieldName: String
    ): List<VectorChunk>
}
