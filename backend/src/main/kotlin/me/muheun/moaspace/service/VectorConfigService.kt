package me.muheun.moaspace.service

import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.dto.VectorConfigCreateRequest
import me.muheun.moaspace.dto.VectorConfigResponse
import me.muheun.moaspace.dto.VectorConfigUpdateRequest
import me.muheun.moaspace.repository.VectorConfigRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * VectorConfig 비즈니스 로직 서비스
 *
 * 벡터 설정의 CRUD 작업 및 캐싱을 담당합니다.
 * 캐시는 5초 TTL로 설정되며, 설정 변경 시 자동으로 무효화됩니다.
 */
@Service
@Transactional(readOnly = true)
class VectorConfigService(
    private val vectorConfigRepository: VectorConfigRepository
) {

    /**
     * 새로운 벡터 설정 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 설정의 응답 DTO
     * @throws IllegalArgumentException 중복된 (entityType, fieldName) 조합인 경우
     */
    @Transactional
    fun create(request: VectorConfigCreateRequest): VectorConfigResponse {
        // 중복 검증
        vectorConfigRepository.findByEntityTypeAndFieldName(request.entityType, request.fieldName)
            ?.let {
                throw IllegalArgumentException(
                    "이미 존재하는 설정입니다: entityType=${request.entityType}, fieldName=${request.fieldName}"
                )
            }

        val config = VectorConfig(
            entityType = request.entityType,
            fieldName = request.fieldName,
            weight = request.weight,
            threshold = request.threshold,
            enabled = request.enabled
        )

        return try {
            val saved = vectorConfigRepository.save(config)
            VectorConfigResponse.from(saved)
        } catch (e: DataIntegrityViolationException) {
            throw IllegalArgumentException(
                "설정 생성 중 오류가 발생했습니다: ${e.message}",
                e
            )
        }
    }

    /**
     * 모든 벡터 설정 조회
     *
     * @return 모든 설정의 응답 DTO 리스트
     */
    fun findAll(): List<VectorConfigResponse> {
        return vectorConfigRepository.findAll()
            .map { VectorConfigResponse.from(it) }
    }

    /**
     * ID로 벡터 설정 조회
     *
     * @param id 설정 ID
     * @return 설정 응답 DTO
     * @throws NoSuchElementException 설정을 찾을 수 없는 경우
     */
    fun findById(id: Long): VectorConfigResponse {
        val config = vectorConfigRepository.findById(id)
            .orElseThrow { NoSuchElementException("설정을 찾을 수 없습니다: id=$id") }
        return VectorConfigResponse.from(config)
    }

    /**
     * 엔티티 타입별 벡터 설정 조회 (캐시 적용)
     *
     * @param entityType 엔티티 타입 (예: Post, Comment)
     * @return 해당 엔티티 타입의 설정 응답 DTO 리스트
     */
    @Cacheable(cacheNames = ["vectorConfig"], key = "#entityType")
    fun findByEntityType(entityType: String): List<VectorConfigResponse> {
        return vectorConfigRepository.findByEntityType(entityType)
            .map { VectorConfigResponse.from(it) }
    }

    /**
     * 엔티티 타입과 필드명으로 벡터 설정 조회 (캐시 적용)
     *
     * @param entityType 엔티티 타입 (예: Post)
     * @param fieldName 필드명 (예: title, content)
     * @return 설정 응답 DTO (없는 경우 null)
     */
    @Cacheable(cacheNames = ["vectorConfig"], key = "#entityType + ':' + #fieldName")
    fun findByEntityTypeAndFieldName(entityType: String, fieldName: String): VectorConfigResponse? {
        return vectorConfigRepository.findByEntityTypeAndFieldName(entityType, fieldName)
            ?.let { VectorConfigResponse.from(it) }
    }

    /**
     * 엔티티 타입의 활성화된 벡터화 설정 조회 (캐시 적용)
     * VectorIndexingService에서 사용하는 핵심 메서드
     *
     * @param entityType 엔티티 타입 (예: Post)
     * @return 활성화된 필드별 설정 리스트 (VectorConfig 엔티티)
     */
    @Cacheable(cacheNames = ["vectorConfig"], key = "#entityType + ':enabled'")
    fun findEnabledConfigsByEntityType(entityType: String): List<VectorConfig> {
        return vectorConfigRepository.findByEntityTypeAndEnabled(entityType, true)
    }

    /**
     * 벡터 설정 수정
     *
     * @param id 설정 ID
     * @param request 수정 요청 DTO (weight, threshold, enabled만 수정 가능)
     * @return 수정된 설정의 응답 DTO
     * @throws NoSuchElementException 설정을 찾을 수 없는 경우
     */
    @Transactional
    @CacheEvict(cacheNames = ["vectorConfig"], allEntries = true)
    fun update(id: Long, request: VectorConfigUpdateRequest): VectorConfigResponse {
        val config = vectorConfigRepository.findById(id)
            .orElseThrow { NoSuchElementException("설정을 찾을 수 없습니다: id=$id") }

        // 변경 가능한 필드만 업데이트 (entityType, fieldName은 유니크 키이므로 변경 불가)
        request.weight?.let { config.weight = it }
        request.threshold?.let { config.threshold = it }
        request.enabled?.let { config.enabled = it }

        val updated = vectorConfigRepository.save(config)
        return VectorConfigResponse.from(updated)
    }

    /**
     * 벡터 설정 삭제
     *
     * @param id 설정 ID
     * @throws NoSuchElementException 설정을 찾을 수 없는 경우
     */
    @Transactional
    @CacheEvict(cacheNames = ["vectorConfig"], allEntries = true)
    fun delete(id: Long) {
        if (!vectorConfigRepository.existsById(id)) {
            throw NoSuchElementException("설정을 찾을 수 없습니다: id=$id")
        }
        vectorConfigRepository.deleteById(id)
    }
}
