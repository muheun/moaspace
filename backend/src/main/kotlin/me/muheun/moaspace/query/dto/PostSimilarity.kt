package me.muheun.moaspace.query.dto

// 게시글 유사도 검색 결과
data class PostSimilarity(
    val postId: Long,
    val title: String,
    val similarity: Double
)
