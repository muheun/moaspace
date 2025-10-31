package me.muheun.moaspace.query.dto

import me.muheun.moaspace.domain.PostEmbedding

/**
 * 게시글 유사도 DTO
 *
 * PostEmbeddingCustomRepository.findSimilarPosts() 반환 타입
 * PostEmbedding과 유사도 스코어를 함께 반환
 *
 * @property postEmbedding PostEmbedding 엔티티 (Post는 Lazy Loading으로 접근)
 * @property similarityScore 코사인 유사도 스코어 (0.0~1.0)
 */
data class PostSimilarity(
    val postEmbedding: PostEmbedding,
    val similarityScore: Double
)
