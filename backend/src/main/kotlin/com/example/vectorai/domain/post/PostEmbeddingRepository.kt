package com.example.vectorai.domain.post

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * PostEmbedding 엔티티 Repository
 *
 * Constitution Principle II: 필드별 벡터화 및 가중치 설정 지원
 * Constitution Principle III: 스코어 임계값 필터링 구현
 * Constitution Principle V: 실제 DB 연동 테스트 필수
 */
@Repository
interface PostEmbeddingRepository : JpaRepository<PostEmbedding, Long> {
    /**
     * Post ID로 임베딩 조회
     */
    fun findByPostId(postId: Long): PostEmbedding?

    /**
     * Post ID로 임베딩 존재 여부 확인
     */
    fun existsByPostId(postId: Long): Boolean

    /**
     * Post ID로 임베딩 삭제 (게시글 수정 시 재생성 전)
     */
    fun deleteByPostId(postId: Long)

    /**
     * 벡터 유사도 검색 (코사인 유사도, 임계값 필터링)
     *
     * Constitution Principle III: 스코어 임계값 필터링
     *
     * @param queryVector 검색 쿼리 벡터 (768차원)
     * @param threshold 유사도 임계값 (0.0~1.0, 기본값 0.6)
     * @param limit 최대 결과 개수
     * @return 유사한 게시글과 유사도 점수 리스트
     */
    @Query(
        value = """
            SELECT pe.post_id, pe.embedding <=> CAST(:queryVector AS vector) AS similarity
            FROM post_embeddings pe
            JOIN posts p ON pe.post_id = p.id
            WHERE p.deleted = FALSE
              AND (1 - (pe.embedding <=> CAST(:queryVector AS vector))) >= :threshold
            ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPosts(
        @Param("queryVector") queryVector: String, // "[0.1, 0.2, ...]" 형식
        @Param("threshold") threshold: Double,
        @Param("limit") limit: Int
    ): List<Array<Any>> // [[postId, similarity], ...]
}
