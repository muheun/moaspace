package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.VectorConfigCreateRequest
import me.muheun.moaspace.dto.VectorConfigResponse
import me.muheun.moaspace.dto.VectorConfigUpdateRequest
import me.muheun.moaspace.service.VectorConfigService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * VectorConfig REST API Controller
 *
 * 벡터 설정 CRUD 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/vector-configs")
class VectorConfigController(
    private val vectorConfigService: VectorConfigService
) {

    /**
     * 새로운 벡터 설정 생성
     *
     * POST /api/vector-configs
     *
     * @param request 생성 요청 DTO (검증 적용)
     * @return 201 Created + 생성된 설정 DTO
     * @throws IllegalArgumentException 중복된 설정인 경우 (409 Conflict로 처리됨)
     */
    @PostMapping
    fun create(@Valid @RequestBody request: VectorConfigCreateRequest): ResponseEntity<VectorConfigResponse> {
        val response = vectorConfigService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 모든 벡터 설정 조회
     *
     * GET /api/vector-configs
     *
     * @return 200 OK + 모든 설정 DTO 리스트
     */
    @GetMapping
    fun findAll(): ResponseEntity<List<VectorConfigResponse>> {
        val responses = vectorConfigService.findAll()
        return ResponseEntity.ok(responses)
    }

    /**
     * ID로 벡터 설정 단건 조회
     *
     * GET /api/vector-configs/{id}
     *
     * @param id 설정 ID
     * @return 200 OK + 설정 DTO
     * @throws NoSuchElementException 설정이 없는 경우 (404 Not Found로 처리됨)
     */
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<VectorConfigResponse> {
        val response = vectorConfigService.findById(id)
        return ResponseEntity.ok(response)
    }

    /**
     * 엔티티 타입과 필드명으로 벡터 설정 조회
     *
     * GET /api/vector-configs/entity/{entityType}/field/{fieldName}
     *
     * @param entityType 엔티티 타입 (예: Post, Comment)
     * @param fieldName 필드명 (예: title, content)
     * @return 200 OK + 설정 DTO, 없는 경우 404 Not Found
     */
    @GetMapping("/entity/{entityType}/field/{fieldName}")
    fun findByEntityTypeAndFieldName(
        @PathVariable entityType: String,
        @PathVariable fieldName: String
    ): ResponseEntity<VectorConfigResponse> {
        val response = vectorConfigService.findByEntityTypeAndFieldName(entityType, fieldName)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response)
    }

    /**
     * 벡터 설정 수정
     *
     * PUT /api/vector-configs/{id}
     *
     * @param id 설정 ID
     * @param request 수정 요청 DTO (검증 적용)
     * @return 200 OK + 수정된 설정 DTO
     * @throws NoSuchElementException 설정이 없는 경우 (404 Not Found로 처리됨)
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: VectorConfigUpdateRequest
    ): ResponseEntity<VectorConfigResponse> {
        val response = vectorConfigService.update(id, request)
        return ResponseEntity.ok(response)
    }

    /**
     * 벡터 설정 삭제
     *
     * DELETE /api/vector-configs/{id}
     *
     * @param id 설정 ID
     * @return 204 No Content
     * @throws NoSuchElementException 설정이 없는 경우 (404 Not Found로 처리됨)
     */
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        vectorConfigService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
