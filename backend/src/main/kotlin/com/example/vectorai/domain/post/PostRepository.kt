package com.example.vectorai.domain.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Post 엔티티 Repository
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필수
 */
@Repository
interface PostRepository : JpaRepository<Post, Long> {
    /**
     * 삭제되지 않은 게시글 목록 조회 (페이지네이션)
     */
    fun findByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post>

    /**
     * 특정 작성자의 게시글 조회 (삭제되지 않은 것만)
     */
    fun findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
        authorId: Long,
        pageable: Pageable
    ): Page<Post>

    /**
     * 해시태그로 게시글 검색 (삭제되지 않은 것만)
     */
    @Query("SELECT p FROM Post p WHERE :hashtag = ANY(p.hashtags) AND p.deleted = false ORDER BY p.createdAt DESC")
    fun findByHashtagAndDeletedFalse(
        @Param("hashtag") hashtag: String,
        pageable: Pageable
    ): Page<Post>

    /**
     * 게시글 ID로 조회 (삭제되지 않은 것만)
     */
    fun findByIdAndDeletedFalse(id: Long): Post?
}
