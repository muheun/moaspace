package com.example.vectorboard.dto

import com.example.vectorboard.domain.Post
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 게시글 생성 요청 DTO
 */
data class PostCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val content: String,

    @field:NotBlank(message = "작성자는 필수입니다")
    @field:Size(max = 100, message = "작성자명은 100자를 초과할 수 없습니다")
    val author: String
)

/**
 * 게시글 수정 요청 DTO
 */
data class PostUpdateRequest(
    @field:Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    val title: String?,

    val content: String?
)

/**
 * 게시글 응답 DTO
 */
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val hasVector: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id!!,
                title = post.title,
                content = post.content,
                author = post.author,
                hasVector = post.contentVector != null,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt
            )
        }
    }
}

/**
 * 벡터 검색 요청 DTO
 */
data class VectorSearchRequest(
    @field:NotBlank(message = "검색어는 필수입니다")
    val query: String,

    val limit: Int = 10
)

/**
 * 벡터 검색 결과 DTO
 */
data class VectorSearchResult(
    val post: PostResponse,
    val similarityScore: Double? = null
)
