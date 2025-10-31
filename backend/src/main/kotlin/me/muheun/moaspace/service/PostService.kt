package me.muheun.moaspace.service

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 게시글 서비스
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

        // XSS 방어: Title sanitize
        val sanitizedTitle = sanitizeTitle(request.title)

        // XSS 방어: Hashtags sanitize
        val sanitizedHashtags = request.hashtags.map { sanitizeHashtag(it) }.toTypedArray()

        // Markdown → HTML 파싱
        val contentHtml = parseMarkdown(request.contentMarkdown)

        // HTML Sanitize (허용된 태그만)
        val sanitizedHtml = sanitizeHtml(contentHtml)

        // HTML → PlainText 추출 (벡터화용)
        val contentText = extractPlainText(sanitizedHtml)

        val post = Post(
            title = sanitizedTitle,
            contentMarkdown = request.contentMarkdown,
            contentHtml = sanitizedHtml,
            contentText = contentText,
            author = author,
            hashtags = sanitizedHashtags
        )

        val savedPost = postRepository.save(post)
        logger.info("게시글 저장 완료: postId=${savedPost.id}")

        postVectorService.vectorizePost(savedPost)
        logger.info("게시글 벡터화 완료: postId=${savedPost.id}")

        return savedPost
    }

    /**
     * Title XSS 방어 - HTML 태그 제거
     */
    private fun sanitizeTitle(title: String): String {
        return title.replace(Regex("<[^>]*>"), "").trim()
    }

    /**
     * Hashtag XSS 방어 - HTML 태그 제거
     */
    private fun sanitizeHashtag(hashtag: String): String {
        return hashtag.replace(Regex("<[^>]*>"), "").trim()
    }

    /**
     * Markdown → HTML 파싱
     */
    private fun parseMarkdown(markdown: String): String {
        val parser = com.vladsch.flexmark.parser.Parser.builder().build()
        val renderer = com.vladsch.flexmark.html.HtmlRenderer.builder().build()
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    /**
     * HTML Sanitize (XSS 방어)
     */
    private fun sanitizeHtml(html: String): String {
        val policy = org.owasp.html.Sanitizers.FORMATTING
            .and(org.owasp.html.Sanitizers.BLOCKS)
            .and(org.owasp.html.Sanitizers.LINKS)
            .and(org.owasp.html.Sanitizers.IMAGES)
            .and(org.owasp.html.Sanitizers.TABLES)
        return policy.sanitize(html)
    }

    /**
     * HTML → 순수 텍스트 추출 (벡터화용)
     */
    private fun extractPlainText(html: String): String {
        // HTML 태그 제거
        val withoutTags = html.replace(Regex("<[^>]*>"), " ")
        // HTML 엔티티 디코드
        val decoded = withoutTags
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        // 연속된 공백 제거
        return decoded.replace(Regex("\\s+"), " ").trim()
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

        // XSS 방어: Title sanitize
        val sanitizedTitle = sanitizeTitle(request.title)

        // XSS 방어: Hashtags sanitize
        val sanitizedHashtags = request.hashtags.map { sanitizeHashtag(it) }.toTypedArray()

        // Markdown → HTML 파싱
        val contentHtml = parseMarkdown(request.contentMarkdown)

        // HTML Sanitize
        val sanitizedHtml = sanitizeHtml(contentHtml)

        // HTML → PlainText 추출
        val contentText = extractPlainText(sanitizedHtml)

        post.title = sanitizedTitle
        post.contentMarkdown = request.contentMarkdown
        post.contentHtml = sanitizedHtml
        post.contentText = contentText
        post.hashtags = sanitizedHashtags

        val updatedPost = postRepository.save(post)
        logger.info("게시글 업데이트 완료: postId=$postId")

        postVectorService.regenerateVector(updatedPost)
        logger.info("게시글 벡터 재생성 완료: postId=$postId")

        return updatedPost
    }

    /**
     * 게시글 목록 조회 (페이지네이션)
     * T061: GET /api/posts 엔드포인트용
     *
     * 삭제되지 않은 게시글만 조회하며, 해시태그 필터링을 지원합니다.
     *
     * @param pageable 페이지 정보 (page, size, sort)
     * @param hashtag 해시태그 필터 (선택적)
     * @return Page<Post> 게시글 페이지
     */
    fun getAllPosts(pageable: Pageable, hashtag: String?): Page<Post> {
        logger.debug("게시글 목록 조회: page=${pageable.pageNumber}, size=${pageable.pageSize}, hashtag=$hashtag")

        return if (hashtag.isNullOrBlank()) {
            postRepository.findByDeletedFalse(pageable)
        } else {
            // Native Query는 Pageable의 Sort를 올바르게 처리하지 못하므로
            // List와 Count를 분리해서 조회 후 수동으로 Page 생성
            val content = postRepository.findByHashtag(
                hashtag = hashtag,
                limit = pageable.pageSize,
                offset = pageable.offset
            )
            val total = postRepository.countByHashtag(hashtag)
            org.springframework.data.domain.PageImpl(content, pageable, total)
        }
    }

    /**
     * 게시글 삭제 (Soft Delete)
     * T076: DELETE /api/posts/{id} 엔드포인트용
     *
     * 1. 게시글 조회 (삭제되지 않은 글만)
     * 2. 소유권 검증 (작성자 본인만 삭제 가능)
     * 3. deleted 플래그를 true로 설정
     * 4. PostEmbedding은 유지 (복구 가능성 고려)
     *
     * @param postId 게시글 ID
     * @param userId 요청자 ID (JWT에서 추출)
     * @throws NoSuchElementException 게시글을 찾을 수 없을 경우
     * @throws IllegalArgumentException 소유권이 없을 경우 (작성자 불일치)
     */
    @Transactional
    fun deletePost(postId: Long, userId: Long) {
        logger.info("게시글 삭제 시작: postId=$postId, userId=$userId")

        val post = getPostById(postId)

        if (post.author.id != userId) {
            logger.warn("게시글 삭제 권한 없음: postId=$postId, authorId=${post.author.id}, requestUserId=$userId")
            throw IllegalArgumentException("게시글을 삭제할 권한이 없습니다")
        }

        post.deleted = true
        postRepository.save(post)

        logger.info("게시글 소프트 삭제 완료: postId=$postId (deleted=true)")
    }
}
