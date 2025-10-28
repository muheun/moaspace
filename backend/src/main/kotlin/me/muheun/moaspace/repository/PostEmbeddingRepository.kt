package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * PostEmbedding 엔티티 Repository
 * 게시글 벡터 임베딩 관리 및 유사도 검색
 */
@Repository
interface PostEmbeddingRepository : JpaRepository<PostEmbedding, Long> {

    /**
     * Post로 PostEmbedding 조회
     * @param post 게시글
     * @return PostEmbedding (Optional)
     */
    fun findByPost(post: Post): Optional<PostEmbedding>

    /**
     * Post ID로 PostEmbedding 존재 여부 확인
     * @param post 게시글
     * @return 존재 여부
     */
    fun existsByPost(post: Post): Boolean

    /**
     * Post로 PostEmbedding 삭제
     * @param post 게시글
     */
    fun deleteByPost(post: Post)

    /**
     * 벡터 유사도 검색 (코사인 거리 기반)
     * Constitution Principle III: 임계값 필터링 지원
     *
     * @param queryVector 검색 벡터 (pgvector 형식)
     * @param threshold 유사도 임계값 (0.0~1.0, 높을수록 엄격)
     * @param limit 반환할 최대 결과 수
     * @return 유사도가 높은 순서로 정렬된 PostEmbedding 리스트
     */
    @Query(
        value = """
            SELECT pe.*,
                   1 - (pe.embedding <=> CAST(:queryVector AS vector)) AS similarity_score
            FROM post_embeddings pe
            JOIN posts p ON pe.post_id = p.id
            WHERE p.deleted = false
              AND 1 - (pe.embedding <=> CAST(:queryVector AS vector)) >= :threshold
            ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPosts(
        @Param("queryVector") queryVector: String,
        @Param("threshold") threshold: Double = 0.6,
        @Param("limit") limit: Int = 20
    ): List<PostEmbedding>
}
