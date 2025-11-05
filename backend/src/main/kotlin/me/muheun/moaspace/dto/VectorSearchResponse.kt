package me.muheun.moaspace.dto

/**
 * 벡터 검색 응답 DTO
 * T063: POST /api/posts/search 엔드포인트용
 *
 * Constitution Principle IX: frontend/types/api/post.ts의 VectorSearchResponse와 수동 동기화 필요
 */
data class VectorSearchResponse(
    val results: List<SearchResult>
)

/**
 * 검색 결과 개별 항목
 * 게시글 정보 + 유사도 점수 포함
 *
 * PostSummary는 PostListResponse.kt에 정의되어 있음
 */
data class SearchResult(
    val post: PostSummary,
    val similarity: Double
)
