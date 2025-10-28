package me.muheun.moaspace.service

import com.pgvector.PGvector
import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.PostEmbedding
import me.muheun.moaspace.repository.PostEmbeddingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = LoggerFactory.getLogger(PostVectorService::class.java)

/**
 * 게시글 벡터화 서비스
 *
 * Post의 plainContent를 768차원 벡터로 변환하여 PostEmbedding으로 저장합니다.
 * Constitution Principle II: 필드별 벡터화 지원
 * Constitution Principle III: 임계값 필터링 지원
 */
@Service
class PostVectorService(
    private val onnxEmbeddingService: OnnxEmbeddingService,
    private val postEmbeddingRepository: PostEmbeddingRepository
) {

    /**
     * 게시글의 plainContent를 벡터화하여 PostEmbedding 생성
     *
     * @param post 벡터화할 게시글
     * @return 생성된 PostEmbedding
     */
    @Transactional
    fun vectorizePost(post: Post): PostEmbedding {
        require(post.id != null) { "Post ID가 null입니다. Post를 먼저 저장해야 합니다." }
        require(post.plainContent.isNotBlank()) { "plainContent가 비어있습니다." }

        logger.info("게시글 벡터화 시작: postId=${post.id}, plainContent 길이=${post.plainContent.length}")

        val embedding: PGvector = try {
            onnxEmbeddingService.generateEmbedding(post.plainContent)
        } catch (e: Exception) {
            logger.error("벡터 생성 실패: postId=${post.id}, error=${e.message}", e)
            throw RuntimeException("게시글 벡터화 실패: ${e.message}", e)
        }

        val postEmbedding = PostEmbedding(
            post = post,
            embedding = embedding.toArray(),
            modelName = "multilingual-e5-base"
        )

        val saved = postEmbeddingRepository.save(postEmbedding)
        logger.info("게시글 벡터화 완료: postId=${post.id}, embeddingId=${saved.id}")

        return saved
    }

    /**
     * 게시글의 벡터를 재생성 (수정 시 사용)
     *
     * 기존 PostEmbedding을 삭제하고 새로 생성합니다.
     *
     * @param post 벡터를 재생성할 게시글
     * @return 재생성된 PostEmbedding
     */
    @Transactional
    fun regenerateVector(post: Post): PostEmbedding {
        require(post.id != null) { "Post ID가 null입니다." }

        logger.info("게시글 벡터 재생성 시작: postId=${post.id}")

        postEmbeddingRepository.findByPost(post).ifPresent { existing ->
            logger.debug("기존 벡터 삭제: embeddingId=${existing.id}")
            postEmbeddingRepository.delete(existing)
        }

        return vectorizePost(post)
    }

    /**
     * 쿼리 텍스트를 벡터로 변환
     *
     * 벡터 검색 시 사용됩니다.
     *
     * @param queryText 검색 쿼리
     * @return 768차원 벡터 (pgvector 문자열 형식)
     */
    fun vectorizeQuery(queryText: String): String {
        require(queryText.isNotBlank()) { "쿼리가 비어있습니다." }

        logger.debug("쿼리 벡터화: queryText=${queryText.take(50)}...")

        val embedding = onnxEmbeddingService.generateEmbedding(queryText)

        return "[${embedding.toArray().joinToString(",")}]"
    }

    /**
     * 벡터 유사도 검색
     *
     * Constitution Principle III: 임계값 이상의 결과만 반환
     *
     * @param queryText 검색 쿼리
     * @param threshold 유사도 임계값 (0.0~1.0, 기본 0.6)
     * @param limit 최대 결과 수 (기본 20)
     * @return 유사한 게시글의 PostEmbedding 리스트
     */
    @Transactional(readOnly = true)
    fun searchSimilarPosts(
        queryText: String,
        threshold: Double = 0.6,
        limit: Int = 20
    ): List<PostEmbedding> {
        require(threshold in 0.0..1.0) { "threshold는 0.0~1.0 사이여야 합니다: $threshold" }
        require(limit > 0) { "limit는 0보다 커야 합니다: $limit" }

        logger.info("벡터 검색 시작: query=${queryText.take(50)}, threshold=$threshold, limit=$limit")

        val queryVector = vectorizeQuery(queryText)
        val results = postEmbeddingRepository.findSimilarPosts(queryVector, threshold, limit)

        logger.info("벡터 검색 완료: 결과 수=${results.size}")

        return results
    }
}
