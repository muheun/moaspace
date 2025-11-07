package me.muheun.moaspace.mapper

import me.muheun.moaspace.domain.post.Post
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface PostMapper {

    /**
     * 해시태그 개수 조회
     * PostgreSQL ANY 배열 연산
     */
    fun countByHashtag(@Param("hashtag") hashtag: String): Long

    /**
     * 해시태그로 게시글 검색
     * PostgreSQL ANY 배열 연산 + 페이지네이션
     */
    fun findByHashtag(
        @Param("hashtag") hashtag: String,
        @Param("deleted") deleted: Boolean,
        @Param("limit") limit: Int,
        @Param("offset") offset: Long
    ): List<Post>

    /**
     * 해시태그 검색 전체 개수
     */
    fun countByHashtagForSearch(
        @Param("hashtag") hashtag: String,
        @Param("deleted") deleted: Boolean
    ): Long
}
