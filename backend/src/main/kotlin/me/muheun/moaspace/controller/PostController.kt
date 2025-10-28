package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.PostResponse
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.service.JwtTokenService
import me.muheun.moaspace.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * 게시글 컨트롤러
 * T043-046: 게시글 CRUD 엔드포인트 구현
 *
 * 주요 엔드포인트:
 * - POST /api/posts: 게시글 생성 (JWT 인증 필요)
 * - GET /api/posts/{id}: 게시글 조회 (JWT 인증 필요)
 * - PUT /api/posts/{id}: 게시글 수정 (JWT 인증 + 소유권 검증)
 *
 * Constitution Principle IX: PostResponse는 frontend/types/api/posts.ts와 수동 동기화
 */
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
    private val jwtTokenService: JwtTokenService
) {

    private val logger = LoggerFactory.getLogger(PostController::class.java)

    /**
     * 게시글 생성
     * T043: POST /api/posts 엔드포인트 구현
     *
     * JWT 토큰에서 사용자 ID를 추출하여 게시글을 생성합니다.
     * 생성 즉시 PostVectorService를 통해 자동 벡터화됩니다.
     *
     * @param authorization "Bearer {token}" 형식의 JWT 토큰
     * @param request 게시글 생성 요청 (title, content, plainContent, hashtags)
     * @return PostResponse (생성된 게시글 정보)
     * @throws IllegalArgumentException JWT 토큰이 없거나 유효하지 않을 경우 (401)
     * @throws NoSuchElementException 작성자를 찾을 수 없을 경우 (404)
     */
    @PostMapping
    fun createPost(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: CreatePostRequest
    ): ResponseEntity<PostResponse> {
        val userId = extractUserIdFromToken(authorization)

        logger.info("게시글 생성 요청: userId=$userId, title=${request.title}")

        val post = postService.createPost(request, userId)
        val response = PostResponse.from(post)

        logger.info("게시글 생성 완료: postId=${post.id}, userId=$userId")

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 게시글 조회
     * T044: GET /api/posts/{id} 엔드포인트 구현
     *
     * 삭제되지 않은 게시글만 조회 가능합니다.
     *
     * @param id 게시글 ID
     * @param authorization JWT 토큰 (인증 확인용)
     * @return PostResponse (게시글 상세 정보)
     * @throws IllegalArgumentException JWT 토큰이 없거나 유효하지 않을 경우 (401)
     * @throws NoSuchElementException 게시글을 찾을 수 없거나 삭제된 경우 (404)
     */
    @GetMapping("/{id}")
    fun getPostById(
        @PathVariable id: Long,
        @RequestHeader("Authorization", required = false) authorization: String?
    ): ResponseEntity<PostResponse> {
        extractUserIdFromToken(authorization)

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
     * @param id 게시글 ID
     * @param authorization "Bearer {token}" 형식의 JWT 토큰
     * @param request 게시글 수정 요청 (title, content, plainContent, hashtags)
     * @return PostResponse (수정된 게시글 정보)
     * @throws IllegalArgumentException JWT 토큰이 없거나 유효하지 않을 경우 (401)
     * @throws IllegalArgumentException 소유권이 없을 경우 (403)
     * @throws NoSuchElementException 게시글을 찾을 수 없을 경우 (404)
     */
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: UpdatePostRequest
    ): ResponseEntity<PostResponse> {
        val userId = extractUserIdFromToken(authorization)

        logger.info("게시글 수정 요청: postId=$id, userId=$userId")

        val updatedPost = postService.updatePost(id, request, userId)
        val response = PostResponse.from(updatedPost)

        logger.info("게시글 수정 완료: postId=$id, userId=$userId")

        return ResponseEntity.ok(response)
    }

    /**
     * Authorization 헤더에서 JWT 토큰을 추출하고 사용자 ID 반환
     *
     * @param authorization "Bearer {token}" 형식
     * @return 사용자 ID
     * @throws IllegalArgumentException 토큰이 없거나 유효하지 않을 경우
     */
    private fun extractUserIdFromToken(authorization: String?): Long {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            logger.warn("Authorization 헤더가 없거나 형식이 잘못되었습니다: $authorization")
            throw IllegalArgumentException("인증이 필요합니다")
        }

        val token = jwtTokenService.extractToken(authorization)
            ?: throw IllegalArgumentException("JWT 토큰 형식이 잘못되었습니다")

        if (!jwtTokenService.validateToken(token)) {
            logger.warn("JWT 토큰이 유효하지 않습니다")
            throw IllegalArgumentException("JWT 토큰이 유효하지 않습니다")
        }

        return jwtTokenService.getUserIdFromToken(token)
    }

    /**
     * 예외 처리: 인증 오류 (401 Unauthorized)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleAuthenticationException(ex: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        logger.error("인증 또는 권한 오류: ${ex.message}")

        val errorResponse = mapOf(
            "error" to mapOf(
                "code" to if (ex.message?.contains("권한") == true) "FORBIDDEN" else "UNAUTHORIZED",
                "message" to (ex.message ?: "인증이 필요합니다"),
                "timestamp" to LocalDateTime.now()
            )
        )

        val status = if (ex.message?.contains("권한") == true) {
            HttpStatus.FORBIDDEN
        } else {
            HttpStatus.UNAUTHORIZED
        }

        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * 예외 처리: 게시글 없음 (404 Not Found)
     */
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
