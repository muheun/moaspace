package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.query.dto.PostSearchFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Post 엔티티에 대한 Kotlin JDSL 기반 Custom Repository 인터페이스
 *
 * Constitution Principle VI 준수:
 * - Native Query 제거
 * - Kotlin JDSL로 타입 안전 쿼리 작성
 * - PostgreSQL 배열 연산자는 customPredicate()로 처리
 *
 * JPA CustomRepository 패턴:
 * 1. 이 인터페이스: 순수 메서드 시그니처 정의 (계약)
 * 2. PostCustomRepositoryImpl: 실제 Kotlin JDSL 쿼리 구현
 * 3. PostRepository: JpaRepository + Custom 통합
 *
 * Phase: 2
 */
interface PostCustomRepository {

    /**
     * 통합 동적 검색 (제목, 작성자, 해시태그, 삭제 여부)
     *
     * 기존 메서드들:
     * - findByDeletedFalse(pageable)
     * - findByAuthorAndDeletedFalse(author, pageable)
     * - findByTitleContainingIgnoreCaseAndDeletedFalse(title, pageable)
     * - findByHashtag (Native Query - PostgreSQL 배열)
     *
     * Kotlin JDSL 통합:
     * - whereAnd() 패턴으로 동적 조건 조합
     * - customPredicate()로 ANY 배열 연산자 처리
     * - 메서드 수 75% 감소 (Success Criteria SC-002)
     *
     * @param filter 검색 필터 (nullable 필드는 검색 조건에서 제외)
     * @param pageable 페이지네이션 및 정렬
     * @return 페이지네이션된 게시글 목록
     */
    fun search(
        filter: PostSearchFilter,
        pageable: Pageable
    ): Page<Post>

    /**
     * 해시태그 개수 조회
     *
     * 기존 Native Query:
     * ```sql
     * SELECT COUNT(*) FROM posts p
     * WHERE :hashtag = ANY(p.hashtags)
     *   AND p.deleted = false
     * ```
     *
     * Kotlin JDSL 변환:
     * - customPredicate()로 ANY 연산자 처리
     *
     * @param hashtag 해시태그
     * @return 해당 해시태그를 가진 게시글 수
     */
    fun countByHashtag(hashtag: String): Long
}
