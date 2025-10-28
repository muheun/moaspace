package me.muheun.moaspace.controller

import me.muheun.moaspace.dto.UserResponse
import me.muheun.moaspace.service.JwtTokenService
import me.muheun.moaspace.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 인증 컨트롤러
 * T031: GET /api/auth/me 엔드포인트 구현
 * T032: POST /api/auth/logout 엔드포인트 구현
 *
 * 주요 엔드포인트:
 * - GET /api/auth/me: 현재 로그인한 사용자 정보 조회 (JWT 기반)
 * - POST /api/auth/logout: 로그아웃 (클라이언트 측 토큰 삭제)
 *
 * Constitution Principle IX: UserResponse는 frontend/types/api/user.ts와 수동 동기화
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtTokenService: JwtTokenService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * 현재 로그인한 사용자 정보 조회
     * T031: GET /api/auth/me 엔드포인트 구현
     *
     * JWT 토큰에서 사용자 ID를 추출하여 사용자 정보를 반환합니다.
     * 프론트엔드에서 헤더 표시 및 로그인 상태 확인에 사용됩니다.
     *
     * @param authorization "Bearer {token}" 형식의 JWT 토큰
     * @return UserResponse (사용자 정보)
     * @throws NoSuchElementException 사용자가 존재하지 않을 경우 (404)
     * @throws IllegalArgumentException JWT 토큰이 없거나 형식이 잘못된 경우 (401)
     */
    @GetMapping("/me")
    fun getCurrentUser(
        @RequestHeader("Authorization", required = false) authorization: String?
    ): ResponseEntity<UserResponse> {
        // Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            logger.warn("Authorization 헤더가 없거나 형식이 잘못되었습니다: $authorization")
            throw IllegalArgumentException("인증이 필요합니다")
        }

        // JWT 토큰 추출
        val token = jwtTokenService.extractToken(authorization)
            ?: throw IllegalArgumentException("JWT 토큰 형식이 잘못되었습니다")

        // 토큰 유효성 검증
        if (!jwtTokenService.validateToken(token)) {
            logger.warn("JWT 토큰이 유효하지 않습니다")
            throw IllegalArgumentException("JWT 토큰이 유효하지 않습니다")
        }

        // 토큰에서 사용자 ID 추출
        val userId = jwtTokenService.getUserIdFromToken(token)

        logger.info("현재 사용자 정보 조회: userId=$userId")

        // 사용자 조회 및 응답 변환
        val user = userService.getUserById(userId)
        val response = UserResponse.from(user)

        return ResponseEntity.ok(response)
    }

    /**
     * 로그아웃
     * T032: POST /api/auth/logout 엔드포인트 구현
     *
     * Stateless JWT 방식이므로 서버 측에서는 특별한 처리가 불필요합니다.
     * 프론트엔드에서 localStorage의 JWT 토큰을 삭제하면 로그아웃 완료됩니다.
     *
     * 향후 JWT 블랙리스트 기능 추가 시 이 엔드포인트를 확장할 수 있습니다.
     *
     * @return 204 No Content
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
        logger.info("로그아웃 요청 (클라이언트 측 JWT 토큰 삭제)")

        // Stateless JWT 방식: 서버 측 처리 불필요
        // 프론트엔드에서 localStorage.removeItem('access_token') 호출

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    /**
     * 예외 처리: 인증 오류
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleAuthenticationException(ex: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        logger.error("인증 오류: ${ex.message}")

        val errorResponse = mapOf(
            "error" to mapOf(
                "code" to "UNAUTHORIZED",
                "message" to (ex.message ?: "인증이 필요합니다"),
                "timestamp" to java.time.LocalDateTime.now()
            )
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    /**
     * 예외 처리: 사용자 없음
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleUserNotFoundException(ex: NoSuchElementException): ResponseEntity<Map<String, Any>> {
        logger.error("사용자 없음: ${ex.message}")

        val errorResponse = mapOf(
            "error" to mapOf(
                "code" to "USER_NOT_FOUND",
                "message" to (ex.message ?: "사용자를 찾을 수 없습니다"),
                "timestamp" to java.time.LocalDateTime.now()
            )
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
}
