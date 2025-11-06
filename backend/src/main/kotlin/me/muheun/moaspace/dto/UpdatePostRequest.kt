package me.muheun.moaspace.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다")
    val title: String,

    @field:NotBlank(message = "HTML 내용은 필수입니다")
    val contentHtml: String,

    @field:Size(max = 10, message = "해시태그는 최대 10개입니다")
    val hashtags: List<@Size(min = 1, max = 50, message = "해시태그는 1~50자 사이여야 합니다") String> = emptyList()
)
