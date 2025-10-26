package me.muheun.moaspace.domain

import me.muheun.moaspace.config.PGvectorType
import com.pgvector.PGvector
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDateTime

/**
 * 콘텐츠 청크 엔티티
 * 긴 게시글을 작은 청크로 분할하여 벡터 검색 품질 향상
 */
@Entity
@Table(
    name = "content_chunks",
    indexes = [
        Index(name = "idx_content_chunk_post_id", columnList = "post_id"),
        Index(name = "idx_content_chunk_index", columnList = "post_id, chunk_index")
    ]
)
class ContentChunk(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    val chunkText: String,

    @Column(name = "chunk_vector", columnDefinition = "vector(768)")
    @Type(PGvectorType::class)
    var chunkVector: PGvector? = null,

    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,

    @Column(name = "start_position", nullable = false)
    val startPosition: Int,

    @Column(name = "end_position", nullable = false)
    val endPosition: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun toString(): String {
        return "ContentChunk(id=$id, postId=${post.id}, chunkIndex=$chunkIndex, " +
                "textLength=${chunkText.length}, range=$startPosition-$endPosition)"
    }
}
