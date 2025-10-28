package me.muheun.moaspace.service

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.dto.*
import me.muheun.moaspace.repository.PostRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val postVectorAdapter: PostVectorAdapter
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
     * 1. Post 엔티티 저장
     * 2. PostVectorAdapter를 통해 범용 벡터 시스템에 인덱싱 요청
     * 3. 즉시 응답 반환 ⚡
     * 4. 백그라운드에서 벡터 생성 및 청크 저장 (비동기)
     */
    @Transactional
    fun createPost(request: PostCreateRequest): PostResponse {
        // 1. 게시글 엔티티 생성 및 저장
        val post = Post(
            title = request.title,
            content = request.content,
            plainContent = "",  // PostVectorAdapter에서 변환 처리
            author = request.author
        )

        val savedPost = postRepository.save(post)

        // 2. 백그라운드에서 비동기로 벡터 생성 (즉시 응답!)
        postVectorAdapter.indexPost(savedPost)

        // 3. 즉시 응답 반환 (벡터 생성을 기다리지 않음)
        return PostResponse.from(savedPost)
    }

    /**
     * 게시글 수정 (비동기)
     *
     * 처리 과정:
     * 1. 제목/내용 업데이트
     * 2. PostVectorAdapter를 통해 재인덱싱 요청
     * 3. 즉시 응답 반환 ⚡
     * 4. 내용 변경 시 백그라운드에서 청크 재생성 (비동기)
     */
    @Transactional
    fun updatePost(id: Long, request: PostUpdateRequest): PostResponse {
        val post = postRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")

        var needsReindex = false

        // 제목 업데이트
        request.title?.let {
            post.title = it
            needsReindex = true
        }

        // 내용 업데이트
        request.content?.let { newContent ->
            post.content = newContent
            needsReindex = true
        }

        val updatedPost = postRepository.save(post)

        // 변경사항이 있으면 재인덱싱
        if (needsReindex) {
            postVectorAdapter.reindexPost(updatedPost)
        }

        return PostResponse.from(updatedPost)
    }

    /**
     * 게시글 삭제
     *
     * 처리 과정:
     * 1. 벡터 인덱스에서 삭제 (동기)
     * 2. Post 엔티티 삭제
     */
    @Transactional
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw NoSuchElementException("게시글을 찾을 수 없습니다: id=$id")
        }

        // 벡터 인덱스에서 삭제
        postVectorAdapter.deletePost(id)

        // Post 엔티티 삭제
        postRepository.deleteById(id)
    }

    /**
     * 벡터 유사도 기반 검색
     *
     * 처리 과정:
     * 1. PostVectorAdapter를 통해 범용 검색 실행
     * 2. title 60%, content 40% 가중치 자동 적용
     * 3. 검색 결과 반환
     */
    fun searchSimilarPosts(request: PostVectorSearchRequest): List<PostVectorSearchResult> {
        return postVectorAdapter.searchPosts(request)
    }

    /**
     * 제목으로 검색
     */
    fun searchByTitle(title: String): List<PostResponse> {
        return postRepository.findByTitleContainingIgnoreCase(title)
            .map { PostResponse.from(it) }
    }
}
