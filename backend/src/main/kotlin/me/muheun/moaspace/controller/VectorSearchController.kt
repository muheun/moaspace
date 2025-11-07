package me.muheun.moaspace.controller

import jakarta.validation.Valid
import me.muheun.moaspace.dto.VectorSearchRequest
import me.muheun.moaspace.service.VectorSearchService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
