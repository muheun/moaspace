package me.muheun.moaspace.controller

import me.muheun.moaspace.dto.UserResponse
import me.muheun.moaspace.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

/**
 * 인증 컨트롤러
 * T031: GET /api/auth/me 엔드포인트 구현
 * T032: POST /api/auth/logout 엔드포인트 구현
 *
 * 주요 엔드포인트:
 * - GET /api/auth/me: 현재 로그인한 사용자 정보 조회 (JWT 기반, Spring Security Filter Chain 자동 검증)
 * - POST /api/auth/logout: 로그아웃 (클라이언트 측 토큰 삭제)
 *
 * Spring Security OAuth2 Resource Server 표준 방식:
 * - Filter Chain에서 JWT 자동 검증 (NimbusJwtDecoder)
 * - @AuthenticationPrincipal로 SecurityContext에서 인증 정보 주입
 * - 컨트롤러 레벨의 수동 JWT 검증 제거
 *
 * Constitution Principle IX: UserResponse는 frontend/types/api/user.ts와 수동 동기화
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * 현재 로그인한 사용자 정보 조회
     * T031: GET /api/auth/me 엔드포인트 구현
     *
     * Spring Security Filter Chain이 JWT 토큰을 자동으로 검증하고,
     * @AuthenticationPrincipal을 통해 인증된 사용자 정보를 주입합니다.
     *
     * 프론트엔드에서 헤더 표시 및 로그인 상태 확인에 사용됩니다.
     *
     * @param jwt Spring Security가 검증한 JWT 토큰 (자동 주입)
     * @return UserResponse (사용자 정보)
     * @throws NoSuchElementException 사용자가 존재하지 않을 경우 (404)
     */
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserResponse> {
        // JWT Claims에서 사용자 ID 추출 (subject에 userId 저장됨)
        val userId = jwt.subject.toLong()

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
     * 예외 처리: 사용자 없음
     *
     * 인증 오류 (@ExceptionHandler(IllegalArgumentException::class))는 제거:
     * - Spring Security Filter Chain이 자동으로 401 응답 처리
     * - AuthenticationEntryPoint가 인증 실패 시 표준 응답 생성
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
