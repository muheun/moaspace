package com.example.vectorboard.service

import com.example.vectorboard.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기존 데이터 마이그레이션 서비스
 * 기존 Post 데이터를 청킹 시스템으로 마이그레이션
 */
@Service
class DataMigrationService(
    private val postRepository: PostRepository,
    private val markdownService: MarkdownService,
    private val vectorProcessingService: VectorProcessingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 기존 Post 데이터를 청킹 시스템으로 마이그레이션
     *
     * 실행 방법:
     * POST /api/admin/migrate-posts (컨트롤러 생성 필요)
     * 또는 애플리케이션 시작 시 자동 실행 (@PostConstruct 사용)
     */
    @Transactional
    fun migrateExistingPosts(): MigrationResult {
        logger.info("=== 기존 Post 데이터 마이그레이션 시작 ===")

        val allPosts = postRepository.findAll()
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        allPosts.forEach { post ->
            try {
                logger.info("마이그레이션 중: Post ID=${post.id}, Title='${post.title}'")

                // 1. plainContent가 없으면 생성
                if (post.plainContent == null) {
                    post.plainContent = markdownService.toPlainText(post.content)
                    logger.debug("  - plainContent 생성 완료 (${post.plainContent?.length} 문자)")
                }

                // 2. 백그라운드에서 비동기로 청크 생성
                vectorProcessingService.processChunksAsync(post, post.plainContent ?: "")

                logger.info("  ✅ Post ID=${post.id} 마이그레이션 시작 (백그라운드 처리)")
                successCount++

            } catch (e: Exception) {
                logger.error("  ❌ Post ID=${post.id} 마이그레이션 실패: ${e.message}", e)
                failureCount++
                errors.add("Post ID=${post.id}: ${e.message}")
            }
        }

        val result = MigrationResult(
            totalPosts = allPosts.size,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )

        logger.info("=== 마이그레이션 완료 ===")
        logger.info("전체: ${result.totalPosts}, 성공: ${result.successCount}, 실패: ${result.failureCount}")

        return result
    }

    /**
     * 특정 Post만 마이그레이션 (비동기)
     */
    @Transactional
    fun migrateSinglePost(postId: Long): Boolean {
        logger.info("Post ID=$postId 마이그레이션 시작")

        val post = postRepository.findById(postId).orElse(null)
            ?: throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$postId")

        try {
            // plainContent 생성
            if (post.plainContent == null) {
                post.plainContent = markdownService.toPlainText(post.content)
            }

            // 백그라운드에서 비동기로 청크 재생성
            vectorProcessingService.reprocessChunksAsync(post, post.plainContent ?: "")

            logger.info("✅ Post ID=$postId 마이그레이션 시작 (백그라운드 처리)")
            return true

        } catch (e: Exception) {
            logger.error("❌ Post ID=$postId 마이그레이션 실패: ${e.message}", e)
            throw e
        }
    }
}

/**
 * 마이그레이션 결과 DTO
 */
data class MigrationResult(
    val totalPosts: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
)
