package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.Post
import java.time.LocalDateTime

/**
 * 게시글 응답 DTO
 */
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: AuthorInfo,
    val hashtags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                author = AuthorInfo(
                    id = post.author.id!!,
                    name = post.author.name,
                    profileImageUrl = post.author.profileImageUrl
                ),
                hashtags = post.hashtags.toList(),
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

/**
 * 게시글 작성자 정보 DTO
 */
data class AuthorInfo(
    val id: Long,
    val name: String,
    val profileImageUrl: String?
)
