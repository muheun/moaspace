package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.PostEmbedding

/**
 * 벡터 검색 응답 DTO
 * T063: POST /api/posts/search 엔드포인트용
 *
 * Constitution Principle IX: frontend/types/api/post.ts의 VectorSearchResponse와 수동 동기화 필요
 */
data class VectorSearchResponse(
    val results: List<SearchResult>
) {
    companion object {
        /**
         * PostEmbedding 리스트로부터 VectorSearchResponse 생성
         *
         * 주의: Native Query에서 similarity_score는 PostEmbedding 엔티티에 직접 매핑되지 않으므로,
         * 별도로 계산하여 전달해야 합니다.
         */
        fun from(postEmbeddings: List<PostEmbedding>, similarities: Map<Long, Double>): VectorSearchResponse {
            return VectorSearchResponse(
                results = postEmbeddings.map { embedding ->
                    SearchResult.from(embedding, similarities[embedding.id] ?: 0.0)
                }
            )
        }
    }
}

/**
 * 검색 결과 개별 항목
 * 게시글 정보 + 유사도 점수 포함
 */
data class SearchResult(
    val post: PostSummary, // 게시글 요약 정보
    val similarity: Double // 유사도 점수 (0.0~1.0, 1.0에 가까울수록 유사)
) {
    companion object {
        fun from(postEmbedding: PostEmbedding, similarity: Double): SearchResult {
            return SearchResult(
                post = PostSummary.from(postEmbedding.post),
                similarity = similarity
            )
        }
    }
}
