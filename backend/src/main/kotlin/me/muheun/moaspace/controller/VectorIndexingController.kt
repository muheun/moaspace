package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.VectorIndexRequest
import me.muheun.moaspace.service.VectorIndexingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 수동 벡터 인덱싱 API
 *
 * 벡터 인덱스 생성, 재생성, 삭제를 수동으로 제어하는 REST API입니다.
 */
@RestController
@RequestMapping("/api/vector-indexing")
class VectorIndexingController(
    private val vectorIndexingService: VectorIndexingService
) {

    /**
     * 엔티티 벡터 인덱싱
     *
     * POST /api/vector-indexing/index
     *
     * @param request 인덱싱 요청 DTO (entity, recordKey, fields)
     * @return 201 Created + 생성된 청크 개수
     */
    @PostMapping("/index")
    fun indexEntity(@Valid @RequestBody request: VectorIndexRequest): ResponseEntity<Map<String, Any>> {
        val chunkCount = vectorIndexingService.indexEntity(
            entityType = request.entity,
            recordKey = request.recordKey,
            fields = request.fields,
            namespace = request.namespace
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "message" to "벡터 인덱싱 완료",
                "chunkCount" to chunkCount
            )
        )
    }

    /**
     * 엔티티 벡터 재인덱싱
     *
     * POST /api/vector-indexing/reindex
     *
     * @param request 재인덱싱 요청 DTO
     * @return 200 OK + 재생성된 청크 개수
     */
    @PostMapping("/reindex")
    fun reindexEntity(@Valid @RequestBody request: VectorIndexRequest): ResponseEntity<Map<String, Any>> {
        val chunkCount = vectorIndexingService.reindexEntity(
            entityType = request.entity,
            recordKey = request.recordKey,
            fields = request.fields,
            namespace = request.namespace
        )
        return ResponseEntity.ok(
            mapOf(
                "message" to "벡터 재인덱싱 완료",
                "chunkCount" to chunkCount
            )
        )
    }

    /**
     * 엔티티 벡터 인덱스 삭제
     *
     * DELETE /api/vector-indexing/{entity}/{recordKey}
     *
     * @param entity 엔티티 타입
     * @param recordKey 레코드 식별자
     * @param namespace 네임스페이스 (기본값: vector_ai)
     * @return 204 No Content
     */
    @DeleteMapping("/{entity}/{recordKey}")
    fun deleteEntityIndex(
        @PathVariable entity: String,
        @PathVariable recordKey: String,
        @RequestParam(defaultValue = "vector_ai") namespace: String
    ): ResponseEntity<Void> {
        vectorIndexingService.deleteEntityIndex(entity, recordKey, namespace)
        return ResponseEntity.noContent().build()
    }
}
