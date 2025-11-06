package me.muheun.moaspace.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class ErrorResponse(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val status: Int,
        val error: String,
        val message: String,
        val details: Map<String, String>? = null
    )

    /**
     * 엔티티를 찾을 수 없을 때
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "리소스를 찾을 수 없습니다"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    /**
     * 비즈니스 로직 위반 (중복 생성 등)
     *
     * VectorConfig 중복 생성 시 409 Conflict 반환
     * 기타 비즈니스 로직 위반 시 400 Bad Request 반환
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        // VectorConfig 중복 생성 감지
        val isDuplicateVectorConfig = ex.message?.contains("이미 존재하는 설정입니다") == true

        val status = if (isDuplicateVectorConfig) HttpStatus.CONFLICT else HttpStatus.BAD_REQUEST
        val errorResponse = ErrorResponse(
            status = status.value(),
            error = if (isDuplicateVectorConfig) "Conflict" else "Bad Request",
            message = ex.message ?: "잘못된 요청입니다"
        )
        return ResponseEntity.status(status).body(errorResponse)
    }

    /**
     * 유효성 검사 실패 시
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.associate {
            val fieldName = (it as? FieldError)?.field ?: "unknown"
            val errorMessage = it.defaultMessage ?: "유효하지 않은 값입니다"
            fieldName to errorMessage
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = "입력값 검증에 실패했습니다",
            details = errors
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    /**
     * 권한 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        logger.warn("권한 거부: ${ex.message}")

        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = "접근 권한이 없습니다"
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    /**
     * 런타임 예외 처리 (벡터 인덱싱 실패 등)
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        logger.error("런타임 오류 발생: ${ex.javaClass.simpleName} - ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Runtime Error",
            message = ex.message ?: "처리 중 오류가 발생했습니다"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    /**
     * 기타 모든 예외
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("서버 내부 오류 발생: ${ex.javaClass.simpleName} - ${ex.message}", ex)

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "서버 내부 오류가 발생했습니다"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
