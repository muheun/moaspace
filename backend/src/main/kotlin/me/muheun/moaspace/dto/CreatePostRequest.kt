package me.muheun.moaspace.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시글 생성 요청 DTO
 *
 * Constitution Principle VIII: Markdown만 받고 서버에서 HTML + PlainText 자동 생성
 * - 클라이언트: contentMarkdown (Markdown) 전송
 * - 서버:
 *   1. contentMarkdown (원본 저장)
 *   2. Markdown → HTML 파싱 → contentHtml (화면 표시용)
 *   3. HTML → 태그 제거 → contentText (벡터화용)
 */
data class CreatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    val contentMarkdown: String,

    @field:Size(max = 10, message = "해시태그는 최대 10개입니다")
    val hashtags: List<@Size(min = 1, max = 50, message = "해시태그는 1~50자 사이여야 합니다") String> = emptyList()
)
