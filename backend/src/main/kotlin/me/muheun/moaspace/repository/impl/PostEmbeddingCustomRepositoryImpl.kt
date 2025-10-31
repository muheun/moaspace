package me.muheun.moaspace.repository.impl

import me.muheun.moaspace.mapper.PostEmbeddingMapper
import me.muheun.moaspace.query.dto.PostSimilarity
import me.muheun.moaspace.repository.PostEmbeddingCustomRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

// 게시글 벡터 유사도 검색 (MyBatis)
@Repository
class PostEmbeddingCustomRepositoryImpl(
    private val postEmbeddingMapper: PostEmbeddingMapper
) : PostEmbeddingCustomRepository {

    companion object {
        private val logger = LoggerFactory.getLogger(PostEmbeddingCustomRepositoryImpl::class.java)
    }

    // 벡터 유사도 검색 (MyBatis - pgvector <=> 연산자)
    override fun findSimilarPosts(
        queryVector: FloatArray,
        threshold: Double,
        limit: Int
    ): List<PostSimilarity> {
        logger.debug("findSimilarPosts 호출: vectorSize={}, threshold={}, limit={}",
            queryVector.size, threshold, limit)

        // FloatArray → PostgreSQL vector 문자열 형식
        val vectorString = "[${queryVector.joinToString(",")}]"

        val results = postEmbeddingMapper.findSimilarPosts(vectorString, threshold, limit)

        logger.info("findSimilarPosts 완료: 검색된 게시글 수={}, threshold={}, 상위 유사도={}",
            results.size, threshold, results.firstOrNull()?.similarity)

        return results
    }
}
