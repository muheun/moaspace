package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.Post
import java.time.LocalDateTime

/**
 * 게시글 응답 DTO
 *
 * Frontend로 전달하는 데이터:
 * - contentMarkdown: 에디터 편집용 (Markdown 원본)
 * - contentHtml: 화면 표시용 (HTML 렌더링)
 */
data class PostResponse(
    val id: Long,
    val title: String,
    val contentMarkdown: String,
    val contentHtml: String,
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
                contentMarkdown = post.contentMarkdown,
                contentHtml = post.contentHtml,
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
