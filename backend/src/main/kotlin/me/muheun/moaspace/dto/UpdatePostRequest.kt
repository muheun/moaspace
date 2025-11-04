package me.muheun.moaspace.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시글 수정 요청 DTO
 *
 * Constitution Principle VIII 개선: Lexical 에디터에서 생성한 HTML을 서버에서 처리
 * - 클라이언트: contentHtml (Lexical 생성) 전송
 * - 서버:
 *   1. contentHtml 수신 (Lexical에서 생성한 HTML, sanitize 적용)
 *   2. HTML → Markdown 변환 (CommonMark, 서버 측 변환)
 *   3. HTML → Plain Text 변환 (jsoup, 벡터화용)
 */
data class UpdatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다")
    val title: String,

    @field:NotBlank(message = "HTML 내용은 필수입니다")
    val contentHtml: String,

    @field:Size(max = 10, message = "해시태그는 최대 10개입니다")
    val hashtags: List<@Size(min = 1, max = 50, message = "해시태그는 1~50자 사이여야 합니다") String> = emptyList()
)
