package me.muheun.moaspace.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Post의 plain_content를 벡터화한 768차원 임베딩 저장
 * Constitution Principle II: 필드별 벡터화 및 가중치 설정 지원
 * Constitution Principle IV: 임베딩 모델 변경 시 마이그레이션 계획 필요
 */
@Entity
@Table(name = "post_embeddings")
class PostEmbedding(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", unique = true, nullable = false)
    val post: Post,

    @Column(nullable = false, columnDefinition = "vector(768)")
    val embedding: FloatArray, // 768차원 벡터 (multilingual-e5-base)

    @Column(name = "model_name", length = 50)
    val modelName: String = "multilingual-e5-base",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostEmbedding) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String {
        return "PostEmbedding(id=$id, postId=${post.id}, modelName='$modelName', createdAt=$createdAt)"
    }
}
