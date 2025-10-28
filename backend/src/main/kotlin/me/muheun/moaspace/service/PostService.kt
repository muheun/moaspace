package me.muheun.moaspace.service

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 게시글 서비스
 * T042: 게시글 CRUD 작업을 위한 PostService 생성
 *
 * 주요 기능:
 * - 게시글 생성 (벡터화 자동 트리거)
 * - 게시글 조회 (삭제되지 않은 글만)
 * - 게시글 수정 (소유권 검증 + 벡터 재생성)
 *
 * Constitution Principle V: 실제 DB 연동 테스트 필요
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 */
@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postVectorService: PostVectorService
) {

    private val logger = LoggerFactory.getLogger(PostService::class.java)

    /**
     * 게시글 생성
     *
     * 1. 작성자 조회
     * 2. Post 엔티티 생성 및 저장
     * 3. PostVectorService를 통해 자동 벡터화
     *
     * @param request 게시글 생성 요청 (title, content, plainContent, hashtags)
     * @param userId 작성자 ID (JWT에서 추출)
     * @return 생성된 Post 엔티티
     * @throws NoSuchElementException 작성자를 찾을 수 없을 경우
     */
    @Transactional
    fun createPost(request: CreatePostRequest, userId: Long): Post {
        logger.info("게시글 생성 시작: userId=$userId, title=${request.title}")

        val author = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("작성자를 찾을 수 없습니다: userId=$userId") }

        val post = Post(
            title = request.title,
            content = request.content,
            plainContent = request.plainContent,
            author = author,
            hashtags = request.hashtags.toTypedArray()
        )

        val savedPost = postRepository.save(post)
        logger.info("게시글 저장 완료: postId=${savedPost.id}")

        postVectorService.vectorizePost(savedPost)
        logger.info("게시글 벡터화 완료: postId=${savedPost.id}")

        return savedPost
    }

    /**
     * 게시글 조회
     *
     * 삭제되지 않은 게시글만 반환합니다.
     *
     * @param postId 게시글 ID
     * @return Post 엔티티
     * @throws NoSuchElementException 게시글을 찾을 수 없거나 삭제된 경우
     */
    fun getPostById(postId: Long): Post {
        logger.debug("게시글 조회: postId=$postId")

        val post = postRepository.findById(postId)
            .orElseThrow { NoSuchElementException("게시글을 찾을 수 없습니다: postId=$postId") }

        if (post.deleted) {
            logger.warn("삭제된 게시글 접근 시도: postId=$postId")
            throw NoSuchElementException("게시글을 찾을 수 없습니다: postId=$postId")
        }

        return post
    }

    /**
     * 게시글 수정
     *
     * 1. 게시글 조회 (삭제되지 않은 글만)
     * 2. 소유권 검증 (작성자 본인만 수정 가능)
     * 3. Post 엔티티 업데이트
     * 4. PostVectorService를 통해 벡터 재생성
     *
     * @param postId 게시글 ID
     * @param request 게시글 수정 요청
     * @param userId 요청자 ID (JWT에서 추출)
     * @return 수정된 Post 엔티티
     * @throws NoSuchElementException 게시글을 찾을 수 없을 경우
     * @throws IllegalArgumentException 소유권이 없을 경우 (작성자 불일치)
     */
    @Transactional
    fun updatePost(postId: Long, request: UpdatePostRequest, userId: Long): Post {
        logger.info("게시글 수정 시작: postId=$postId, userId=$userId")

        val post = getPostById(postId)

        if (post.author.id != userId) {
            logger.warn("게시글 수정 권한 없음: postId=$postId, authorId=${post.author.id}, requestUserId=$userId")
            throw IllegalArgumentException("게시글을 수정할 권한이 없습니다")
        }

        post.title = request.title
        post.content = request.content
        post.plainContent = request.plainContent
        post.hashtags = request.hashtags.toTypedArray()

        val updatedPost = postRepository.save(post)
        logger.info("게시글 업데이트 완료: postId=$postId")

        postVectorService.regenerateVector(updatedPost)
        logger.info("게시글 벡터 재생성 완료: postId=$postId")

        return updatedPost
    }
}
