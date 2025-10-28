package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Post 엔티티 Repository
 * Board/Forum 시스템용 게시글 관리
 */
@Repository
interface PostRepository : JpaRepository<Post, Long> {

    /**
     * 삭제되지 않은 모든 게시글 조회 (페이지네이션)
     * @param pageable 페이지 정보 (정렬, 크기, 페이지 번호)
     * @return 게시글 페이지
     */
    fun findByDeletedFalse(pageable: Pageable): Page<Post>

    /**
     * 작성자별 게시글 조회 (삭제되지 않은 글만)
     * @param author 작성자
     * @param pageable 페이지 정보
     * @return 게시글 페이지
     */
    fun findByAuthorAndDeletedFalse(author: User, pageable: Pageable): Page<Post>

    /**
     * 해시태그로 게시글 검색 (삭제되지 않은 글만)
     * PostgreSQL 배열 연산자 사용을 위해 native query 필요
     * @param hashtag 검색할 해시태그
     * @param pageable 페이지 정보
     * @return 게시글 페이지
     */
    @Query(
        value = """
            SELECT * FROM posts p
            WHERE :hashtag = ANY(p.hashtags)
            AND p.deleted = false
        """,
        countQuery = """
            SELECT COUNT(*) FROM posts p
            WHERE :hashtag = ANY(p.hashtags)
            AND p.deleted = false
        """,
        nativeQuery = true
    )
    fun findByHashtagAndDeletedFalse(
        @Param("hashtag") hashtag: String,
        pageable: Pageable
    ): Page<Post>

    /**
     * 제목으로 게시글 검색 (삭제되지 않은 글만)
     * @param title 검색할 제목 (부분 일치)
     * @param pageable 페이지 정보
     * @return 게시글 페이지
     */
    fun findByTitleContainingIgnoreCaseAndDeletedFalse(
        title: String,
        pageable: Pageable
    ): Page<Post>

    /**
     * 게시글 ID로 조회 (삭제 여부 무관 - 직접 URL 접근 시 삭제 여부 확인용)
     * @param id 게시글 ID
     * @return 게시글 (Optional)
     */
    override fun findById(id: Long): java.util.Optional<Post>
}
