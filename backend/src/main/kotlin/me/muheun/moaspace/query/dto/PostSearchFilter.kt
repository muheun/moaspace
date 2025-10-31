package me.muheun.moaspace.query.dto

import me.muheun.moaspace.domain.user.User

/**
 * 게시글 검색 필터 DTO
 *
 * PostCustomRepository.search() 파라미터 타입
 * 동적 필터링을 위한 nullable 필드 제공
 *
 * @property title 제목 검색어 (부분 일치, 대소문자 무시)
 * @property author 작성자 필터
 * @property hashtag 해시태그 필터 (PostgreSQL ANY 연산자 사용)
 * @property deleted 삭제 여부 필터 (기본값 false)
 */
data class PostSearchFilter(
    val title: String? = null,
    val author: User? = null,
    val hashtag: String? = null,
    val deleted: Boolean = false
)
