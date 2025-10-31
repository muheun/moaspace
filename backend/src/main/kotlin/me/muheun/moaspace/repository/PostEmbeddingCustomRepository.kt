package me.muheun.moaspace.repository

import me.muheun.moaspace.query.dto.PostSimilarity

/**
 * PostEmbedding 엔티티에 대한 Kotlin JDSL 기반 Custom Repository 인터페이스
 *
 * Constitution Principle VI 준수:
 * - Native Query 제거
 * - Kotlin JDSL로 타입 안전 쿼리 작성
 * - pgvector 연산자는 VectorJpql Custom DSL로 래핑
 *
 * JPA CustomRepository 패턴:
 * 1. 이 인터페이스: 순수 메서드 시그니처 정의 (계약)
 * 2. PostEmbeddingCustomRepositoryImpl: 실제 Kotlin JDSL 쿼리 구현
 * 3. PostEmbeddingRepository: JpaRepository + Custom 통합
 *
 * Phase: 2
 */
interface PostEmbeddingCustomRepository {

    /**
     * 벡터 유사도 검색 (JOIN + 임계값 필터링)
     *
     * 기존 Native Query:
     * ```sql
     * SELECT pe.*, 1 - (pe.embedding <=> CAST(:queryVector AS vector)) AS similarity_score
     * FROM post_embeddings pe
     * JOIN posts p ON pe.post_id = p.id
     * WHERE p.deleted = false
     *   AND 1 - (pe.embedding <=> CAST(:queryVector AS vector)) >= :threshold
     * ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
     * LIMIT :limit
     * ```
     *
     * Kotlin JDSL 변환:
     * - VectorJpql.cosineSimilarity() 사용
     * - Post와 JOIN
     * - threshold 임계값 필터링 (Constitution Principle III)
     *
     * @param queryVector 검색 벡터 (768차원)
     * @param threshold 최소 유사도 임계값 (0.0~1.0, 기본값 0.6)
     * @param limit 결과 개수 제한
     * @return 유사도 스코어 포함 PostSimilarity 목록
     */
    fun findSimilarPosts(
        queryVector: FloatArray,
        threshold: Double = 0.6,
        limit: Int
    ): List<PostSimilarity>
}
