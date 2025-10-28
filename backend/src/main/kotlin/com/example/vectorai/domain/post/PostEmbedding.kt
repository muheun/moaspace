package com.example.vectorai.domain.post

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 게시글 벡터 임베딩 엔티티
 *
 * Constitution Principle IV: 임베딩 모델 변경 시 마이그레이션 계획 필요
 * Constitution Principle V: 실제 DB 연동 테스트 대상
 *
 * 768차원 벡터 (multilingual-e5-base 모델 사용)
 */
@Entity
@Table(name = "post_embeddings")
data class PostEmbedding(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", unique = true, nullable = false)
    val post: Post,

    @Column(nullable = false, columnDefinition = "vector(768)")
    val embedding: FloatArray, // 768차원 벡터

    @Column(length = 50)
    val modelName: String = "multilingual-e5-base",

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // FloatArray equality를 위한 equals/hashCode 오버라이드
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostEmbedding

        if (id != other.id) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (modelName != other.modelName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + modelName.hashCode()
        return result
    }
}
