package com.example.vectorboard.repository

import com.example.vectorboard.domain.ContentChunk
import com.example.vectorboard.domain.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * ContentChunk 리포지토리
 */
@Repository
interface ContentChunkRepository : JpaRepository<ContentChunk, Long> {

    /**
     * 특정 Post의 모든 청크 조회 (순서대로)
     */
    fun findByPostOrderByChunkIndexAsc(post: Post): List<ContentChunk>

    /**
     * 특정 Post의 모든 청크 삭제
     */
    fun deleteByPost(post: Post)

    /**
     * 벡터 유사도 기반 청크 검색 (Post 그룹화)
     *
     * 각 Post별로 가장 유사도가 높은 청크의 점수를 사용하여
     * Post를 유사도 순으로 정렬
     *
     * @param queryVector 검색 쿼리의 벡터 (문자열 형식)
     * @param limit 반환할 Post 개수
     * @return Post ID와 유사도 점수 쌍의 리스트
     */
    @Query(
        value = """
            SELECT
                c.post_id as postId,
                MAX(1 - (c.chunk_vector <=> CAST(:queryVector AS vector))) as score
            FROM content_chunks c
            WHERE c.chunk_vector IS NOT NULL
            GROUP BY c.post_id
            ORDER BY score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPostsByChunks(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int
    ): List<PostSimilarityScore>

    /**
     * 벡터 유사도 기반 청크 검색 (청크별 상세 정보 포함)
     *
     * 각 Post별로 가장 유사도가 높은 청크 1개와 함께 반환
     *
     * @param queryVector 검색 쿼리의 벡터
     * @param limit 반환할 결과 개수
     * @return 청크 ID, Post ID, 유사도 점수를 포함한 결과
     */
    @Query(
        value = """
            WITH ranked_chunks AS (
                SELECT
                    c.id,
                    c.post_id,
                    1 - (c.chunk_vector <=> CAST(:queryVector AS vector)) as score,
                    ROW_NUMBER() OVER (PARTITION BY c.post_id ORDER BY (1 - (c.chunk_vector <=> CAST(:queryVector AS vector))) DESC) as rank
                FROM content_chunks c
                WHERE c.chunk_vector IS NOT NULL
            )
            SELECT
                id as chunkId,
                post_id as postId,
                score
            FROM ranked_chunks
            WHERE rank = 1
            ORDER BY score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTopChunksByPost(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int
    ): List<ChunkSearchResult>
}

/**
 * Post 유사도 점수 인터페이스 (네이티브 쿼리 결과 매핑용)
 */
interface PostSimilarityScore {
    fun getPostId(): Long
    fun getScore(): Double
}

/**
 * 청크 검색 결과 인터페이스 (네이티브 쿼리 결과 매핑용)
 */
interface ChunkSearchResult {
    fun getChunkId(): Long
    fun getPostId(): Long
    fun getScore(): Double
}
