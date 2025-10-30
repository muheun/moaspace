package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.*
import me.muheun.moaspace.service.PostService
import me.muheun.moaspace.service.PostVectorService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 게시글 컨트롤러
 * T043-046, T061-062: 게시글 CRUD 및 조회 엔드포인트 구현
 *
 * 주요 엔드포인트:
 * - POST /api/posts: 게시글 생성 (JWT 인증 필요)
 * - GET /api/posts: 게시글 목록 조회 (페이지네이션, 해시태그 필터)
 * - GET /api/posts/{id}: 게시글 조회 (JWT 인증 필요)
 * - PUT /api/posts/{id}: 게시글 수정 (JWT 인증 + 소유권 검증)
 *
 * Spring Security OAuth2 Resource Server 표준 방식:
 * - Filter Chain에서 JWT 자동 검증 (NimbusJwtDecoder)
 * - @AuthenticationPrincipal로 SecurityContext에서 인증 정보 주입
 * - 컨트롤러 레벨의 수동 JWT 검증 제거
 *
 * Constitution Principle IX: PostResponse는 frontend/types/api/posts.ts와 수동 동기화
 */
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
    private val postVectorService: PostVectorService
) {

    private val logger = LoggerFactory.getLogger(PostController::class.java)

    /**
     * 게시글 생성
     * T043: POST /api/posts 엔드포인트 구현
     *
     * Spring Security Filter Chain이 JWT 토큰을 자동으로 검증하고,
     * @AuthenticationPrincipal을 통해 인증된 사용자 정보를 주입합니다.
     * 생성 즉시 PostVectorService를 통해 자동 벡터화됩니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param request 게시글 생성 요청 (title, content, plainContent, hashtags)
     * @return PostResponse (생성된 게시글 정보)
     * @throws NoSuchElementException 작성자를 찾을 수 없을 경우 (404)
     */
    @PostMapping
    fun createPost(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreatePostRequest
    ): ResponseEntity<PostResponse> {
        val userId = jwt.subject.toLong()

        logger.info("게시글 생성 요청: userId=$userId, title=${request.title}")

        val post = postService.createPost(request, userId)
        val response = PostResponse.from(post)

        logger.info("게시글 생성 완료: postId=${post.id}, userId=$userId")

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 게시글 목록 조회 (페이지네이션 + 해시태그 필터)
     * T061-T062: GET /api/posts 엔드포인트 구현
     *
     * 삭제되지 않은 게시글만 조회하며, 최신순으로 정렬됩니다.
     * 해시태그 파라미터가 제공되면 해당 해시태그를 포함하는 게시글만 반환합니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param page 페이지 번호 (0-based, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @param hashtag 해시태그 필터 (선택적)
     * @return PostListResponse (게시글 목록 + 페이지네이션 정보)
     */
    @GetMapping
    fun getAllPosts(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) hashtag: String?
    ): ResponseEntity<PostListResponse> {
        val pageSize = minOf(size, 100)
        val pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))

        logger.info("게시글 목록 조회 요청: page=$page, size=$pageSize, hashtag=$hashtag")

        val postPage = postService.getAllPosts(pageable, hashtag)
        val response = PostListResponse.from(postPage)

        logger.info("게시글 목록 조회 완료: totalElements=${postPage.totalElements}, totalPages=${postPage.totalPages}")

        return ResponseEntity.ok(response)
    }

    /**
     * 게시글 조회
     * T044: GET /api/posts/{id} 엔드포인트 구현
     *
     * 삭제되지 않은 게시글만 조회 가능합니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param id 게시글 ID
     * @return PostResponse (게시글 상세 정보)
     * @throws NoSuchElementException 게시글을 찾을 수 없거나 삭제된 경우 (404)
     */
    @GetMapping("/{id}")
    fun getPostById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ): ResponseEntity<PostResponse> {
        logger.info("게시글 조회 요청: postId=$id")

        val post = postService.getPostById(id)
        val response = PostResponse.from(post)

        return ResponseEntity.ok(response)
    }

    /**
     * 게시글 수정
     * T045: PUT /api/posts/{id} 엔드포인트 구현
     *
     * 작성자 본인만 수정할 수 있습니다.
     * 수정 시 PostVectorService를 통해 벡터가 자동으로 재생성됩니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param id 게시글 ID
     * @param request 게시글 수정 요청 (title, content, plainContent, hashtags)
     * @return PostResponse (수정된 게시글 정보)
     * @throws IllegalArgumentException 소유권이 없을 경우 (403)
     * @throws NoSuchElementException 게시글을 찾을 수 없을 경우 (404)
     */
    @PutMapping("/{id}")
    fun updatePost(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ResponseEntity<PostResponse> {
        val userId = jwt.subject.toLong()

        logger.info("게시글 수정 요청: postId=$id, userId=$userId")

        val updatedPost = postService.updatePost(id, request, userId)
        val response = PostResponse.from(updatedPost)

        logger.info("게시글 수정 완료: postId=$id, userId=$userId")

        return ResponseEntity.ok(response)
    }

    /**
     * 벡터 검색
     * T063-T064: POST /api/posts/search 엔드포인트 구현
     *
     * 게시글의 plainContent를 벡터화하여 유사도 검색을 수행합니다.
     * Constitution Principle III: 임계값 이상의 결과만 반환
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param request 검색 요청 (query, threshold, limit)
     * @return VectorSearchResponse (검색 결과 + 유사도 점수)
     * @throws IllegalArgumentException threshold 또는 limit 값이 유효하지 않을 경우 (400)
     */
    @PostMapping("/search")
    fun searchPosts(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: PostSearchRequest
    ): ResponseEntity<VectorSearchResponse> {
        logger.info("벡터 검색 요청: query=${request.query.take(50)}, threshold=${request.threshold}, limit=${request.limit}")

        val (postEmbeddings, similarities) = postVectorService.searchSimilarPostsWithScores(
            queryText = request.query,
            threshold = request.threshold,
            limit = request.limit
        )

        val response = VectorSearchResponse.from(postEmbeddings, similarities)

        logger.info("벡터 검색 완료: 결과 수=${response.results.size}")

        return ResponseEntity.ok(response)
    }

    /**
     * 게시글 삭제 (Soft Delete)
     * T076-T077: DELETE /api/posts/{id} 엔드포인트 구현
     *
     * 작성자 본인만 삭제할 수 있습니다.
     * 실제로 데이터를 삭제하지 않고 deleted=true로 설정합니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @param id 게시글 ID
     * @return 204 No Content (삭제 성공)
     * @throws IllegalArgumentException 소유권이 없을 경우 (403)
     * @throws NoSuchElementException 게시글을 찾을 수 없을 경우 (404)
     */
    @DeleteMapping("/{id}")
    fun deletePost(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val userId = jwt.subject.toLong()

        logger.info("게시글 삭제 요청: postId=$id, userId=$userId")

        postService.deletePost(id, userId)

        logger.info("게시글 삭제 완료: postId=$id, userId=$userId")

        return ResponseEntity.noContent().build()
    }

    /**
     * 예외 처리: 권한 오류 (403 Forbidden) 및 리소스 없음 (404 Not Found)
     *
     * 인증 오류 (@ExceptionHandler(IllegalArgumentException::class) - 401)는 제거:
     * - Spring Security Filter Chain이 자동으로 401 응답 처리
     * - AuthenticationEntryPoint가 인증 실패 시 표준 응답 생성
     *
     * 권한 오류 (403 Forbidden)와 리소스 없음 (404 Not Found)는 비즈니스 로직에서 발생하므로 유지:
     * - 소유권 검증 실패: 403 Forbidden
     * - 게시글 또는 사용자 없음: 404 Not Found
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBusinessException(ex: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        logger.error("비즈니스 로직 오류: ${ex.message}")

        val errorResponse = mapOf(
            "error" to mapOf(
                "code" to "FORBIDDEN",
                "message" to (ex.message ?: "권한이 없습니다"),
                "timestamp" to LocalDateTime.now()
            )
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handlePostNotFoundException(ex: NoSuchElementException): ResponseEntity<Map<String, Any>> {
        logger.error("리소스 없음: ${ex.message}")

        val errorResponse = mapOf(
            "error" to mapOf(
                "code" to "POST_NOT_FOUND",
                "message" to (ex.message ?: "게시글을 찾을 수 없습니다"),
                "timestamp" to LocalDateTime.now()
            )
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
}
