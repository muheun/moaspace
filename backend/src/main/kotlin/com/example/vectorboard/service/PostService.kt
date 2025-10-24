package com.example.vectorboard.service

import com.example.vectorboard.domain.Post
import com.example.vectorboard.dto.*
import com.example.vectorboard.repository.ContentChunkRepository
import com.example.vectorboard.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val contentChunkRepository: ContentChunkRepository,
    private val vectorService: VectorService,
    private val markdownService: MarkdownService,
    private val postVectorService: PostVectorService
) {

    /**
     * 모든 게시글 조회
     */
    fun getAllPosts(): List<PostResponse> {
        return postRepository.findAll()
            .map { PostResponse.from(it) }
    }

    /**
     * ID로 게시글 조회
     */
    fun getPostById(id: Long): PostResponse {
        val post = postRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")
        return PostResponse.from(post)
    }

    /**
     * 게시글 생성 (비동기)
     *
     * 처리 과정:
     * 1. 마크다운 → 순수 텍스트 변환
     * 2. Post 엔티티 저장
     * 3. 즉시 응답 반환 ⚡
     * 4. 백그라운드에서 벡터 생성 및 청크 저장 (비동기)
     */
    @Transactional
    fun createPost(request: PostCreateRequest): PostResponse {
        // 1. 마크다운을 순수 텍스트로 변환
        val plainText = markdownService.toPlainText(request.content)

        // 2. 게시글 엔티티 생성 및 저장
        val post = Post(
            title = request.title,
            content = request.content,
            plainContent = plainText,
            author = request.author
        )

        val savedPost = postRepository.save(post)

        // 3. 백그라운드에서 비동기로 벡터 생성 (즉시 응답!)
        postVectorService.processChunksAsync(savedPost, plainText)

        // 4. 즉시 응답 반환 (벡터 생성을 기다리지 않음)
        return PostResponse.from(savedPost)
    }

    /**
     * 게시글 수정 (비동기)
     *
     * 처리 과정:
     * 1. 제목/내용 업데이트
     * 2. 즉시 응답 반환 ⚡
     * 3. 내용 변경 시 백그라운드에서 청크 재생성 (비동기)
     */
    @Transactional
    fun updatePost(id: Long, request: PostUpdateRequest): PostResponse {
        val post = postRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")

        // 제목 업데이트
        request.title?.let { post.title = it }

        // 내용 업데이트 시 비동기로 청크 재생성
        request.content?.let { newContent ->
            // 마크다운을 순수 텍스트로 변환
            val plainText = markdownService.toPlainText(newContent)

            // Post 업데이트
            post.content = newContent
            post.plainContent = plainText

            // 백그라운드에서 비동기로 청크 재생성 (즉시 응답!)
            postVectorService.reprocessChunksAsync(post, plainText)
        }

        val updatedPost = postRepository.save(post)
        return PostResponse.from(updatedPost)
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")
        }
        postRepository.deleteById(id)
    }

    /**
     * 벡터 유사도 기반 검색 (청크 기반)
     *
     * 처리 과정:
     * 1. 검색어 벡터화
     * 2. ContentChunk에서 유사도 검색
     * 3. Post 그룹화 + 최대 유사도 점수 계산
     * 4. 가장 유사한 청크 정보 함께 반환
     */
    fun searchSimilarPosts(request: PostVectorSearchRequest): List<PostVectorSearchResult> {
        // 1. 검색어를 벡터로 변환
        val queryVector = vectorService.generateEmbedding(request.query)
        val queryVectorString = vectorService.vectorToString(queryVector)

        // 2. 청크 기반 검색 (Post별 최고 유사도 청크 조회)
        val chunkResults = contentChunkRepository.findTopChunksByPost(
            queryVectorString,
            request.limit
        )

        // 3. Post 조회 및 결과 구성
        return chunkResults.mapNotNull { result ->
            val post = postRepository.findByIdOrNull(result.getPostId())
            post?.let {
                // 가장 유사한 청크 조회
                val topChunk = contentChunkRepository.findByIdOrNull(result.getChunkId())

                PostVectorSearchResult(
                    post = PostResponse.from(it),
                    similarityScore = result.getScore(),
                    matchedChunkText = topChunk?.chunkText,
                    chunkPosition = topChunk?.let { chunk ->
                        ChunkPosition(
                            startPos = chunk.startPosition,
                            endPos = chunk.endPosition
                        )
                    }
                )
            }
        }
    }

    /**
     * 제목으로 검색
     */
    fun searchByTitle(title: String): List<PostResponse> {
        return postRepository.findByTitleContainingIgnoreCase(title)
            .map { PostResponse.from(it) }
    }
}
