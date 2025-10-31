package me.muheun.moaspace.mapper

import me.muheun.moaspace.query.dto.PostSimilarity
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface PostEmbeddingMapper {

    /**
     * 벡터 유사도 검색
     * pgvector <=> 연산자 (MyBatis XML)
     * queryVector: PostgreSQL vector 문자열 형식 "[1.0,2.0,...]"
     */
    fun findSimilarPosts(
        @Param("queryVector") queryVector: String,
        @Param("threshold") threshold: Double,
        @Param("limit") limit: Int
    ): List<PostSimilarity>
}
