package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.query.dto.PostSearchFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Post Custom Repository (QueryDSL)
 */
interface PostCustomRepository {

    /**
     * 통합 동적 검색
     * QueryDSL BooleanBuilder로 동적 조건 조합
     */
    fun search(
        filter: PostSearchFilter,
        pageable: Pageable
    ): Page<Post>

    /**
     * 해시태그 개수 조회
     * PostgreSQL ANY 배열 연산자 (Expressions.template)
     */
    fun countByHashtag(hashtag: String): Long
}
