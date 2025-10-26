package me.muheun.moaspace.service

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.dto.PostVectorSearchRequest
import me.muheun.moaspace.dto.PostVectorSearchResult
import me.muheun.moaspace.dto.PostResponse
import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.dto.ChunkPosition
import me.muheun.moaspace.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture

/**
 * Post 엔티티와 범용 벡터 시스템 간의 어댑터
 *
 * Post의 title과 content를 독립적으로 벡터화하고, 검색 시 title 60%, content 40% 가중치를 적용합니다.
 */
@Service
class PostVectorAdapter(
    private val universalVectorIndexingService: UniversalVectorIndexingService,
    private val markdownService: MarkdownService,
    private val postRepository: PostRepository
) {

    companion object {
        private const val NAMESPACE = "vector_ai"
        private const val ENTITY = "posts"
        private const val FIELD_TITLE = "title"
        private const val FIELD_CONTENT = "content"
        private const val TITLE_WEIGHT = 0.6
        private const val CONTENT_WEIGHT = 0.4
    }

    /**
     * Post를 벡터 인덱스에 추가
     */
    fun indexPost(post: Post): CompletableFuture<Unit> {
        val plainContent = markdownService.toPlainText(post.content)

        val request = VectorIndexRequest(
            namespace = NAMESPACE,
            entity = ENTITY,
            recordKey = post.id.toString(),
            fields = mapOf(
                FIELD_TITLE to post.title,
                FIELD_CONTENT to plainContent
            ),
            metadata = mapOf(
                "author" to post.author,
                "createdAt" to post.createdAt.toString()
            )
        )

        return universalVectorIndexingService.indexEntity(request)
    }

    /**
     * Post를 재인덱싱
     */
    @Transactional
    fun reindexPost(post: Post): CompletableFuture<Unit> {
        val plainContent = markdownService.toPlainText(post.content)

        val request = VectorIndexRequest(
            namespace = NAMESPACE,
            entity = ENTITY,
            recordKey = post.id.toString(),
            fields = mapOf(
                FIELD_TITLE to post.title,
                FIELD_CONTENT to plainContent
            ),
            metadata = mapOf(
                "author" to post.author,
                "createdAt" to post.createdAt.toString(),
                "updatedAt" to post.updatedAt.toString()
            )
        )

        return universalVectorIndexingService.reindexEntity(request)
    }

    /**
     * Post를 벡터 인덱스에서 삭제
     */
    @Transactional
    fun deletePost(postId: Long) {
        universalVectorIndexingService.deleteEntity(
            namespace = NAMESPACE,
            entity = ENTITY,
            recordKey = postId.toString()
        )
    }

    /**
     * 벡터 유사도 기반 Post 검색
     *
     * title 60%, content 40% 가중치로 검색합니다.
     */
    @Transactional(readOnly = true)
    fun searchPosts(request: PostVectorSearchRequest): List<PostVectorSearchResult> {
        val vectorSearchRequest = VectorSearchRequest(
            query = request.query,
            namespace = NAMESPACE,
            entity = ENTITY,
            fieldName = null,
            fieldWeights = mapOf(
                FIELD_TITLE to TITLE_WEIGHT,
                FIELD_CONTENT to CONTENT_WEIGHT
            ),
            limit = request.limit
        )

        val vectorResults = universalVectorIndexingService.search(vectorSearchRequest)

        return vectorResults.mapNotNull { result ->
            val postId = result.recordKey.toLongOrNull() ?: return@mapNotNull null
            val post = postRepository.findByIdOrNull(postId) ?: return@mapNotNull null

            PostVectorSearchResult(
                post = PostResponse.from(post),
                similarityScore = result.similarityScore,
                matchedChunkText = result.chunkText,
                chunkPosition = ChunkPosition(
                    startPos = result.startPosition ?: 0,
                    endPos = result.endPosition ?: 0
                )
            )
        }
    }

    /**
     * 특정 필드만 검색
     */
    @Transactional(readOnly = true)
    fun searchByField(
        query: String,
        fieldName: String,
        limit: Int = 10
    ): List<PostVectorSearchResult> {
        val vectorSearchRequest = VectorSearchRequest(
            query = query,
            namespace = NAMESPACE,
            entity = ENTITY,
            fieldName = fieldName,
            fieldWeights = null,
            limit = limit
        )

        val vectorResults = universalVectorIndexingService.search(vectorSearchRequest)

        return vectorResults.mapNotNull { result ->
            val postId = result.recordKey.toLongOrNull() ?: return@mapNotNull null
            val post = postRepository.findByIdOrNull(postId) ?: return@mapNotNull null

            PostVectorSearchResult(
                post = PostResponse.from(post),
                similarityScore = result.similarityScore,
                matchedChunkText = result.chunkText,
                chunkPosition = ChunkPosition(
                    startPos = result.startPosition ?: 0,
                    endPos = result.endPosition ?: 0
                )
            )
        }
    }
}
