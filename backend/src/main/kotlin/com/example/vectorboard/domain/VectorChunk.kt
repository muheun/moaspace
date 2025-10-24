package com.example.vectorboard.domain

import com.example.vectorboard.config.PGvectorType
import com.pgvector.PGvector
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * 범용 벡터 청크 엔티티
 *
 * 모든 엔티티(posts, products, comments 등)의 텍스트 필드를 청크 단위로 벡터화하여 저장합니다.
 * namespace/entity/recordKey/fieldName으로 벡터의 출처를 추적하여 범용적인 벡터 검색을 지원합니다.
 *
 * @property id 청크 ID (자동 생성)
 * @property namespace 네임스페이스 (예: "vector_ai", "my_app")
 * @property entity 엔티티 타입 (예: "posts", "products", "comments")
 * @property recordKey 레코드 식별자 (원본 테이블의 ID를 문자열로 저장)
 * @property fieldName 필드명 (예: "title", "content", "description")
 * @property chunkText 청크의 실제 텍스트 내용
 * @property chunkVector 청크의 벡터 임베딩 (1536차원)
 * @property chunkIndex 청크 순서 (0부터 시작)
 * @property startPosition 원본 텍스트에서의 시작 위치
 * @property endPosition 원본 텍스트에서의 끝 위치
 * @property metadata 추가 메타데이터 (JSONB 형식 - 카테고리, 태그, 작성자 등)
 * @property createdAt 생성 시각
 * @property updatedAt 수정 시각
 */
@Entity
@Table(
    name = "vector_chunk",
    indexes = [
        // 특정 레코드의 모든 청크 조회 (재인덱싱 시 삭제용)
        Index(name = "idx_vector_chunk_lookup", columnList = "namespace, entity, record_key"),
        // 특정 필드 검색
        Index(name = "idx_vector_chunk_field", columnList = "namespace, entity, field_name"),
        // 청크 순서 조회
        Index(name = "idx_vector_chunk_order", columnList = "namespace, entity, record_key, chunk_index")
    ]
)
class VectorChunk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // ========================================
    // 범용 메타데이터 필드
    // ========================================

    @Column(name = "namespace", nullable = false, length = 100)
    val namespace: String = "vector_ai",

    @Column(name = "entity", nullable = false, length = 100)
    val entity: String,

    @Column(name = "record_key", nullable = false, length = 255)
    val recordKey: String,

    @Column(name = "field_name", nullable = false, length = 100)
    val fieldName: String,

    // ========================================
    // 청크 내용
    // ========================================

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    val chunkText: String,

    @Column(name = "chunk_vector", columnDefinition = "vector(1536)")
    @Type(PGvectorType::class)
    var chunkVector: PGvector? = null,

    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,

    @Column(name = "start_position", nullable = false)
    val startPosition: Int,

    @Column(name = "end_position", nullable = false)
    val endPosition: Int,

    // ========================================
    // 추가 메타데이터 (JSONB)
    // JSON Object 형식으로 저장
    // ========================================

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any>? = null,

    // ========================================
    // 타임스탬프
    // ========================================

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 엔티티 업데이트 전 자동으로 updatedAt 갱신
     */
    @PreUpdate
    fun onPreUpdate() {
        updatedAt = LocalDateTime.now()
    }

    /**
     * 디버깅용 문자열 표현
     */
    override fun toString(): String {
        return "VectorChunk(" +
                "id=$id, " +
                "namespace='$namespace', " +
                "entity='$entity', " +
                "recordKey='$recordKey', " +
                "fieldName='$fieldName', " +
                "chunkIndex=$chunkIndex, " +
                "textLength=${chunkText.length}, " +
                "range=$startPosition-$endPosition" +
                ")"
    }

    /**
     * 동일성 비교 (id 기반)
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorChunk) return false
        return id != null && id == other.id
    }

    /**
     * 해시코드 (id 기반)
     */
    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
