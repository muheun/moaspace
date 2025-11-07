package me.muheun.moaspace.controller

import me.muheun.moaspace.dto.UserResponse
import me.muheun.moaspace.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    
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
