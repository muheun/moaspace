package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.Post
import org.springframework.data.domain.Page

/**
 * 게시글 목록 응답 DTO (페이지네이션 포함)
 * T061: GET /api/posts 엔드포인트용
 *
 * Constitution Principle IX: frontend/types/api/post.ts의 PostListResponse와 수동 동기화 필요
 */
data class PostListResponse(
    val posts: List<PostSummary>,
    val pagination: PaginationInfo
) {
    companion object {
        fun from(page: Page<Post>): PostListResponse {
            return PostListResponse(
                posts = page.content.map { PostSummary.from(it) },
                pagination = PaginationInfo(
                    page = page.number,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages
                )
            )
        }
    }
}

/**
 * 게시글 요약 정보 (목록용)
 * 전체 content를 포함하지 않고 일부 정보만 제공
 */
data class PostSummary(
    val id: Long,
    val title: String,
    val excerpt: String, // content의 첫 100자 (미리보기)
    val author: AuthorInfo,
    val hashtags: List<String>,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime?
) {
    companion object {
        fun from(post: Post): PostSummary {
            val excerpt = if (post.contentText.length > 100) {
                post.contentText.substring(0, 100) + "..."
            } else {
                post.contentText
            }

            return PostSummary(
                id = post.id!!,
                title = post.title,
                excerpt = excerpt,
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
 * 페이지네이션 정보
 */
data class PaginationInfo(
    val page: Int, // 현재 페이지 (0-based)
    val size: Int, // 페이지 크기
    val totalElements: Long, // 전체 게시글 수
    val totalPages: Int // 전체 페이지 수
)
