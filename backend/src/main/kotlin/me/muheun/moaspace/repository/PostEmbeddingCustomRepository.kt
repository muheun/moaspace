package me.muheun.moaspace.repository

import me.muheun.moaspace.query.dto.PostSimilarity

/**
 * PostEmbedding Custom Repository (QueryDSL)
 */
interface PostEmbeddingCustomRepository {

    /**
     * 벡터 유사도 검색
     * VectorExpression.cosineSimilarity() 사용
     */
    fun findSimilarPosts(
        queryVector: FloatArray,
        threshold: Double = 0.6,
        limit: Int
    ): List<PostSimilarity>
}
