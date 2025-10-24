package com.example.vectorboard.service

import com.example.vectorboard.domain.ContentChunk
import com.example.vectorboard.domain.Post
import com.example.vectorboard.repository.ContentChunkRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Post 전용 벡터 처리 서비스
 *
 * Post 엔티티의 벡터 생성 및 청크 저장을 백그라운드에서 처리합니다.
 * ContentChunk 테이블을 사용하여 Post와 직접 연관된 청크를 관리합니다.
 */
@Service
class PostVectorService(
    private val contentChunkRepository: ContentChunkRepository,
    private val vectorService: VectorService,
    private val chunkingService: ChunkingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 비동기로 청크 생성 및 벡터 저장
     *
     * @Async를 사용하여 별도 스레드에서 실행
     * 트랜잭션을 새로 시작 (REQUIRES_NEW)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun processChunksAsync(post: Post, plainContent: String) {
        try {
            logger.info("🔵 비동기 벡터 생성 시작: Post ID=${post.id}, plainContent 길이=${plainContent.length}")

            // 1. 텍스트 청킹
            val chunks = chunkingService.chunkDocument(plainContent)
            logger.info("🟢 청크 생성 완료: ${chunks.size}개 (Post ID=${post.id})")

            // 2. 병렬로 벡터 생성
            val contentChunks = runBlocking {
                chunks.map { chunk ->
                    async(Dispatchers.IO) {
                        val vector = vectorService.generateEmbedding(chunk.text)

                        ContentChunk(
                            post = post,
                            chunkText = chunk.text,
                            chunkVector = vector,
                            chunkIndex = chunk.index,
                            startPosition = chunk.startPos,
                            endPosition = chunk.endPos
                        )
                    }
                }.awaitAll()
            }

            // 3. Batch Insert
            contentChunkRepository.saveAll(contentChunks)

            logger.info("✅ 비동기 벡터 생성 완료: Post ID=${post.id}, ${chunks.size}개 청크 DB 저장 완료")

        } catch (e: Exception) {
            logger.error("❌ 비동기 벡터 생성 실패: Post ID=${post.id}, error=${e.message}", e)
            // 예외를 던지지 않고 로깅만 (비동기 작업 실패가 전체 요청을 실패시키지 않도록)
        }
    }

    /**
     * 비동기로 청크 재생성 (업데이트 시)
     * 기존 청크를 삭제하고 새로 생성
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reprocessChunksAsync(post: Post, plainContent: String) {
        try {
            logger.info("비동기 청크 재생성 시작: Post ID=${post.id}")

            // 1. 기존 청크 삭제
            contentChunkRepository.deleteByPost(post)
            logger.debug("기존 청크 삭제 완료")

            // 2. 새 청크 생성
            processChunksAsync(post, plainContent)

        } catch (e: Exception) {
            logger.error("비동기 청크 재생성 실패: Post ID=${post.id}, error=${e.message}", e)
        }
    }
}
