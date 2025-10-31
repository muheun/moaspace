package me.muheun.moaspace.query.dto

/**
 * 청크 상세 정보 DTO
 *
 * VectorChunkCustomRepository.findTopChunksByRecord() 반환 타입
 * ROW_NUMBER() OVER를 사용한 Window Function 쿼리 결과
 *
 * Kotlin JDSL 마이그레이션:
 * - Native Query → CustomRepository 패턴
 * - 타입 안전한 Kotlin data class 사용
 * - UniversalVectorIndexingService에서 직접 사용
 *
 * @property chunkId 청크 ID
 * @property namespace 네임스페이스 (예: "post")
 * @property entity 엔티티 타입 (예: "Post")
 * @property recordKey 레코드 식별자
 * @property fieldName 필드명 (예: "title", "content")
 * @property score 코사인 유사도 스코어
 */
data class ChunkDetail(
    val chunkId: Long,
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val fieldName: String,
    val score: Double
)
