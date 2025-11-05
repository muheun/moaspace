package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.service.VectorSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 범용 벡터 검색 API (T029)
 *
 * 필드별 가중치 기반 멀티필드 검색을 제공합니다.
 * vector_configs 테이블 설정에 따라 동적으로 가중치 및 임계값이 적용됩니다.
 *
 * Constitution Principle II: 필드별 가중치 설정 지원
 * Constitution Principle III: 스코어 임계값 필터링
 */
@RestController
@RequestMapping("/api/vector-search")
class VectorSearchController(
    private val vectorSearchService: VectorSearchService
) {

    /**
     * 벡터 검색
     *
     * POST /api/vector-search
     *
     * @param request 검색 요청 DTO (query, namespace, entity, fieldName, fieldWeights, limit)
     * @return Map<String, Double> (recordKey -> 최종 가중치 스코어)
     */
    @PostMapping
    fun search(@Valid @RequestBody request: VectorSearchRequest): ResponseEntity<Map<String, Double>> {
        val results = vectorSearchService.search(request)
        return ResponseEntity.ok(results)
    }
}
