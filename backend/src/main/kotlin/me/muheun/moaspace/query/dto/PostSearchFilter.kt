package me.muheun.moaspace.query.dto

// 게시글 검색 필터
data class PostSearchFilter(
    val title: String? = null,
    val author: String? = null,
    val hashtag: String? = null,
    val deleted: Boolean? = null
)
