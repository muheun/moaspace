package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.config.VectorProperties
import me.muheun.moaspace.domain.vector.VectorEntityType
import me.muheun.moaspace.dto.*
import me.muheun.moaspace.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService,
    private val vectorProperties: VectorProperties,
    private val vectorChunkRepository: me.muheun.moaspace.repository.VectorChunkRepository,
    private val postRepository: me.muheun.moaspace.repository.PostRepository,
    private val vectorEmbeddingService: me.muheun.moaspace.service.VectorEmbeddingService
) {

    private val logger = LoggerFactory.getLogger(PostController::class.java)

    
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

    
    @PostMapping("/search")
    fun searchPosts(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: PostSearchRequest
    ): ResponseEntity<VectorSearchResponse> {
        logger.info("벡터 검색 요청: query=${request.query.take(50)}, threshold=${request.threshold}, limit=${request.limit}")

        // 1. 쿼리 벡터화
        val queryVector = vectorEmbeddingService.generateEmbedding(request.query).toArray()

        // 2. VectorChunk 기반 레코드별 유사도 검색
        val recordScores = vectorChunkRepository.findSimilarRecords(
            queryVector = queryVector,
            namespace = vectorProperties.namespace,
            entity = VectorEntityType.POST.typeName,
            limit = request.limit
        )

        // 3. threshold 필터링 및 Post 조회
        val results = recordScores
            .filter { it.score >= request.threshold }
            .mapNotNull { scoreDto ->
                val postId = scoreDto.recordKey.toLongOrNull() ?: return@mapNotNull null
                postRepository.findById(postId).map { post ->
                    SearchResult(
                        post = PostSummary.from(post),
                        similarity = scoreDto.score
                    )
                }.orElse(null)
            }

        val response = VectorSearchResponse(results = results)

        logger.info("벡터 검색 완료: 결과 수=${response.results.size}")

        return ResponseEntity.ok(response)
    }

    
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
