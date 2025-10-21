package com.example.vectorboard.repository

import com.example.vectorboard.domain.Post
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Long> {

    /**
     * 벡터 유사도 기반 검색
     * pgvector의 <-> 연산자는 L2 거리(유클리디안 거리)를 계산합니다
     * <=> 연산자는 코사인 거리를 계산합니다
     *
     * @param queryVector 검색 벡터
     * @param limit 반환할 최대 결과 수
     * @return 유사도가 높은 순서로 정렬된 게시글 리스트
     */
    @Query(
        value = """
            SELECT p.*,
                   p.content_vector <=> CAST(:queryVector AS vector) AS similarity_score
            FROM posts p
            WHERE p.content_vector IS NOT NULL
            ORDER BY p.content_vector <=> CAST(:queryVector AS vector)
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findSimilarPosts(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int = 10
    ): List<Post>

    /**
     * 제목으로 검색
     */
    fun findByTitleContainingIgnoreCase(title: String): List<Post>

    /**
     * 작성자로 검색
     */
    fun findByAuthor(author: String): List<Post>
}
