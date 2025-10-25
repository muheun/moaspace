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
 * 기존 PostService API 호환성을 유지하면서 범용 벡터 인덱싱 시스템을 활용합니다.
 * - namespace="vector_ai", entity="posts" 고정
 * - title과 content를 독립적으로 벡터화
 * - title 60%, content 40% 가중치 기본 적용
 *
 * @property universalVectorIndexingService 범용 벡터 인덱싱 서비스
 * @property markdownService 마크다운 변환 서비스
 * @property postRepository Post 저장소
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

        // 필드별 기본 가중치
        private const val TITLE_WEIGHT = 0.6
        private const val CONTENT_WEIGHT = 0.4
    }

    /**
     * Post를 벡터 인덱스에 추가합니다.
     *
     * 처리 과정:
     * 1. 마크다운을 순수 텍스트로 변환
     * 2. title과 content를 독립적으로 벡터화
     * 3. 범용 인덱싱 서비스를 통해 비동기로 인덱싱
     *
     * @param post 인덱싱할 Post 엔티티
     * @return 비동기 완료 Future
     */
    fun indexPost(post: Post): CompletableFuture<Unit> {
        // 마크다운을 순수 텍스트로 변환
        val plainContent = markdownService.toPlainText(post.content)

        // VectorIndexRequest 생성 (title과 content를 독립 필드로)
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
     * Post를 재인덱싱합니다.
     *
     * 처리 과정:
     * 1. 기존 벡터 청크 동기 삭제
     * 2. title과 content를 새로 벡터화
     * 3. 범용 인덱싱 서비스를 통해 비동기로 인덱싱
     *
     * @param post 재인덱싱할 Post 엔티티
     * @return 비동기 완료 Future
     */
    @Transactional
    fun reindexPost(post: Post): CompletableFuture<Unit> {
        // 마크다운을 순수 텍스트로 변환
        val plainContent = markdownService.toPlainText(post.content)

        // VectorIndexRequest 생성
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
     * Post를 벡터 인덱스에서 삭제합니다.
     *
     * 동기 삭제로 즉시 처리됩니다.
     *
     * @param postId 삭제할 Post ID
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
     * 처리 과정:
     * 1. title 60%, content 40% 가중치로 검색
     * 2. 범용 검색 결과를 PostVectorSearchResult로 변환
     * 3. Post 정보와 청크 정보를 함께 반환
     *
     * @param request Post 벡터 검색 요청
     * @return Post 검색 결과 리스트
     */
    @Transactional(readOnly = true)
    fun searchPosts(request: PostVectorSearchRequest): List<PostVectorSearchResult> {
        // 범용 검색 요청 생성 (title 60%, content 40% 가중치)
        val vectorSearchRequest = VectorSearchRequest(
            query = request.query,
            namespace = NAMESPACE,
            entity = ENTITY,
            fieldName = null, // 모든 필드 검색
            fieldWeights = mapOf(
                FIELD_TITLE to TITLE_WEIGHT,
                FIELD_CONTENT to CONTENT_WEIGHT
            ),
            limit = request.limit
        )

        // 범용 검색 실행
        val vectorResults = universalVectorIndexingService.search(vectorSearchRequest)

        // PostVectorSearchResult로 변환
        return vectorResults.mapNotNull { result ->
            // Post 조회
            val postId = result.recordKey.toLongOrNull() ?: return@mapNotNull null
            val post = postRepository.findByIdOrNull(postId) ?: return@mapNotNull null

            // PostVectorSearchResult 생성
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
     * 특정 필드만 검색합니다.
     *
     * @param query 검색어
     * @param fieldName 검색할 필드명 (title 또는 content)
     * @param limit 결과 개수 제한
     * @return Post 검색 결과 리스트
     */
    @Transactional(readOnly = true)
    fun searchByField(
        query: String,
        fieldName: String,
        limit: Int = 10
    ): List<PostVectorSearchResult> {
        // 범용 검색 요청 생성 (단일 필드)
        val vectorSearchRequest = VectorSearchRequest(
            query = query,
            namespace = NAMESPACE,
            entity = ENTITY,
            fieldName = fieldName,
            fieldWeights = null, // 단일 필드이므로 가중치 불필요
            limit = limit
        )

        // 범용 검색 실행
        val vectorResults = universalVectorIndexingService.search(vectorSearchRequest)

        // PostVectorSearchResult로 변환
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
