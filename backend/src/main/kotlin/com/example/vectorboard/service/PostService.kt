package com.example.vectorboard.service

import com.example.vectorboard.domain.Post
import com.example.vectorboard.dto.*
import com.example.vectorboard.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val vectorService: VectorService
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
     * 게시글 생성
     */
    @Transactional
    fun createPost(request: PostCreateRequest): PostResponse {
        // 게시글 엔티티 생성
        val post = Post(
            title = request.title,
            content = request.content,
            author = request.author
        )

        // 내용을 벡터로 변환하여 저장
        post.contentVector = vectorService.generateEmbedding(request.content)

        val savedPost = postRepository.save(post)
        return PostResponse.from(savedPost)
    }

    /**
     * 게시글 수정
     */
    @Transactional
    fun updatePost(id: Long, request: PostUpdateRequest): PostResponse {
        val post = postRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")

        // 제목 업데이트
        request.title?.let { post.title = it }

        // 내용 업데이트 시 벡터도 재생성
        request.content?.let {
            post.content = it
            post.contentVector = vectorService.generateEmbedding(it)
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
     * 벡터 유사도 기반 검색
     */
    fun searchSimilarPosts(request: VectorSearchRequest): List<VectorSearchResult> {
        // 검색어를 벡터로 변환
        val queryVector = vectorService.generateEmbedding(request.query)
        val queryVectorString = vectorService.vectorToString(queryVector)

        // 유사한 게시글 검색
        val similarPosts = postRepository.findSimilarPosts(queryVectorString, request.limit)

        return similarPosts.map { post ->
            VectorSearchResult(
                post = PostResponse.from(post)
            )
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
