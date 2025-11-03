package me.muheun.moaspace.service

import me.muheun.moaspace.dto.VectorConfigCreateRequest
import me.muheun.moaspace.dto.VectorConfigUpdateRequest
import me.muheun.moaspace.repository.VectorConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import kotlin.system.measureTimeMillis

/**
 * VectorConfigService 통합 테스트
 *
 * Constitution Principle V 준수: Real Database Integration
 * - @SpringBootTest로 전체 컨텍스트 로드
 * - 실제 DB 사용 (Mock 금지)
 * - @Transactional로 각 테스트 격리 및 롤백
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VectorConfigServiceTest @Autowired constructor(
    private val vectorConfigService: VectorConfigService,
    private val vectorConfigRepository: VectorConfigRepository,
    private val entityManager: EntityManager
) {

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 vector_configs 테이블 정리
        entityManager.createNativeQuery("TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("create - 새로운 벡터 설정을 생성한다")
    fun testCreate() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0,
            threshold = 0.5,
            enabled = true
        )

        // when
        val response = vectorConfigService.create(request)

        // then
        assertThat(response.id).isGreaterThan(0)
        assertThat(response.entityType).isEqualTo("Post")
        assertThat(response.fieldName).isEqualTo("title")
        assertThat(response.weight).isEqualTo(2.0)
        assertThat(response.threshold).isEqualTo(0.5)
        assertThat(response.enabled).isTrue()

        // DB 저장 확인
        val saved = vectorConfigRepository.findById(response.id)
        assertThat(saved).isPresent
    }

    @Test
    @DisplayName("create - 중복된 설정 생성 시 예외 발생")
    fun testCreateDuplicate() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0
        )
        vectorConfigService.create(request)

        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            vectorConfigService.create(request)
        }
        assertThat(exception.message).contains("이미 존재하는 설정입니다")
    }

    @Test
    @DisplayName("findAll - 모든 벡터 설정을 조회한다")
    fun testFindAll() {
        // given
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "content", weight = 1.0)
        )

        // when
        val responses = vectorConfigService.findAll()

        // then
        assertThat(responses).hasSizeGreaterThanOrEqualTo(2)
        assertThat(responses.map { it.fieldName })
            .contains("title", "content")
    }

    @Test
    @DisplayName("findById - ID로 벡터 설정을 조회한다")
    fun testFindById() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "title", weight = 2.0)
        )

        // when
        val response = vectorConfigService.findById(created.id)

        // then
        assertThat(response.id).isEqualTo(created.id)
        assertThat(response.entityType).isEqualTo("Post")
        assertThat(response.fieldName).isEqualTo("title")
    }

    @Test
    @DisplayName("findById - 존재하지 않는 ID 조회 시 예외 발생")
    fun testFindByIdNotFound() {
        // when & then
        val exception = assertThrows<NoSuchElementException> {
            vectorConfigService.findById(99999L)
        }
        assertThat(exception.message).contains("설정을 찾을 수 없습니다")
    }

    @Test
    @DisplayName("findByEntityType - 엔티티 타입별 설정을 조회한다 (캐시 적용)")
    fun testFindByEntityType() {
        // given
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "content", weight = 1.0)
        )
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Comment", fieldName = "text", weight = 1.5)
        )

        // when
        val postConfigs = vectorConfigService.findByEntityType("Post")

        // then
        assertThat(postConfigs).hasSize(2)
        assertThat(postConfigs.map { it.fieldName })
            .containsExactlyInAnyOrder("title", "content")

        // 캐시 검증 (두 번째 호출은 캐시에서 반환)
        val cachedConfigs = vectorConfigService.findByEntityType("Post")
        assertThat(cachedConfigs).hasSize(2)
    }

    @Test
    @DisplayName("findByEntityTypeAndFieldName - 엔티티+필드로 설정을 조회한다 (캐시 적용)")
    fun testFindByEntityTypeAndFieldName() {
        // given
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "title", weight = 2.0)
        )

        // when
        val response = vectorConfigService.findByEntityTypeAndFieldName("Post", "title")

        // then
        assertThat(response).isNotNull
        assertThat(response?.entityType).isEqualTo("Post")
        assertThat(response?.fieldName).isEqualTo("title")
        assertThat(response?.weight).isEqualTo(2.0)
    }

    @Test
    @DisplayName("findByEntityTypeAndFieldName - 존재하지 않는 설정 조회 시 null 반환")
    fun testFindByEntityTypeAndFieldNameNotFound() {
        // when
        val response = vectorConfigService.findByEntityTypeAndFieldName("Post", "nonexistent")

        // then
        assertThat(response).isNull()
    }

    @Test
    @DisplayName("기본값 검증 - weight와 threshold 기본값이 올바르게 저장된다")
    fun testDefaultValues() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "summary"
            // weight, threshold, enabled는 기본값 사용
        )

        // when
        val response = vectorConfigService.create(request)

        // then
        assertThat(response.weight).isEqualTo(1.0) // 기본값
        assertThat(response.threshold).isEqualTo(0.0) // 기본값
        assertThat(response.enabled).isTrue() // 기본값
    }

    @Test
    @DisplayName("update - 벡터 설정의 weight를 수정한다 (@CacheEvict 적용)")
    fun testUpdateWeight() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "title", weight = 2.0, threshold = 0.5)
        )

        // when
        val updateRequest = VectorConfigUpdateRequest(weight = 3.5)
        val updated = vectorConfigService.update(created.id, updateRequest)

        // then
        assertThat(updated.id).isEqualTo(created.id)
        assertThat(updated.weight).isEqualTo(3.5)
        assertThat(updated.threshold).isEqualTo(0.5) // 변경되지 않음
        assertThat(updated.enabled).isTrue() // 변경되지 않음

        // DB 저장 확인
        val saved = vectorConfigRepository.findById(created.id)
        assertThat(saved.get().weight).isEqualTo(3.5)
    }

    @Test
    @DisplayName("update - 벡터 설정의 threshold를 수정한다")
    fun testUpdateThreshold() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "content", weight = 1.0, threshold = 0.0)
        )

        // when
        val updateRequest = VectorConfigUpdateRequest(threshold = 0.7)
        val updated = vectorConfigService.update(created.id, updateRequest)

        // then
        assertThat(updated.threshold).isEqualTo(0.7)
        assertThat(updated.weight).isEqualTo(1.0) // 변경되지 않음
    }

    @Test
    @DisplayName("update - 벡터 설정의 enabled를 수정한다")
    fun testUpdateEnabled() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "summary", enabled = true)
        )

        // when
        val updateRequest = VectorConfigUpdateRequest(enabled = false)
        val updated = vectorConfigService.update(created.id, updateRequest)

        // then
        assertThat(updated.enabled).isFalse()
    }

    @Test
    @DisplayName("update - 여러 필드를 동시에 수정한다")
    fun testUpdateMultipleFields() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "tags", weight = 1.0, threshold = 0.0)
        )

        // when
        val updateRequest = VectorConfigUpdateRequest(weight = 5.0, threshold = 0.8, enabled = false)
        val updated = vectorConfigService.update(created.id, updateRequest)

        // then
        assertThat(updated.weight).isEqualTo(5.0)
        assertThat(updated.threshold).isEqualTo(0.8)
        assertThat(updated.enabled).isFalse()
    }

    @Test
    @DisplayName("update - 존재하지 않는 설정 수정 시 예외 발생")
    fun testUpdateNotFound() {
        // given
        val updateRequest = VectorConfigUpdateRequest(weight = 3.0)

        // when & then
        val exception = assertThrows<NoSuchElementException> {
            vectorConfigService.update(99999L, updateRequest)
        }
        assertThat(exception.message).contains("설정을 찾을 수 없습니다")
    }

    @Test
    @DisplayName("delete - 벡터 설정을 삭제한다 (@CacheEvict 적용)")
    fun testDelete() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "description", weight = 1.5)
        )

        // when
        vectorConfigService.delete(created.id)

        // then - DB에서 삭제 확인
        val deleted = vectorConfigRepository.findById(created.id)
        assertThat(deleted).isEmpty()

        // 조회 시도 시 예외 발생
        val exception = assertThrows<NoSuchElementException> {
            vectorConfigService.findById(created.id)
        }
        assertThat(exception.message).contains("설정을 찾을 수 없습니다")
    }

    @Test
    @DisplayName("delete - 존재하지 않는 설정 삭제 시 예외 발생")
    fun testDeleteNotFound() {
        // when & then
        val exception = assertThrows<NoSuchElementException> {
            vectorConfigService.delete(99999L)
        }
        assertThat(exception.message).contains("설정을 찾을 수 없습니다")
    }

    @Test
    @DisplayName("캐시 무효화 검증 - update 후 findByEntityType 캐시가 무효화된다")
    fun testCacheEvictionOnUpdate() {
        // given
        val created = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Post", fieldName = "subtitle", weight = 1.0)
        )

        // 첫 번째 조회 (캐시 저장)
        val firstResponse = vectorConfigService.findByEntityType("Post")
        assertThat(firstResponse).isNotEmpty()

        // when - update 시 캐시 무효화
        vectorConfigService.update(created.id, VectorConfigUpdateRequest(weight = 2.5))

        // then - 두 번째 조회 시 DB에서 새로 조회됨 (업데이트된 값 반영)
        val secondResponse = vectorConfigService.findByEntityType("Post")
        val updatedConfig = secondResponse.find { it.id == created.id }
        assertThat(updatedConfig?.weight).isEqualTo(2.5)
    }

    @Test
    @DisplayName("캐시 무효화 검증 - delete 후 findByEntityType 캐시가 무효화된다")
    fun testCacheEvictionOnDelete() {
        // given
        val created1 = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Comment", fieldName = "text", weight = 1.0)
        )
        val created2 = vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Comment", fieldName = "author", weight = 1.5)
        )

        // 첫 번째 조회 (캐시 저장)
        val firstResponse = vectorConfigService.findByEntityType("Comment")
        assertThat(firstResponse).hasSize(2)

        // when - delete 시 캐시 무효화
        vectorConfigService.delete(created1.id)

        // then - 두 번째 조회 시 DB에서 새로 조회됨 (삭제된 항목 제외)
        val secondResponse = vectorConfigService.findByEntityType("Comment")
        assertThat(secondResponse).hasSize(1)
        assertThat(secondResponse[0].id).isEqualTo(created2.id)
    }

    @Test
    @DisplayName("캐시 성능 측정 - 캐시 히트 < 10ms, 캐시 미스 < 100ms (헌법 Principle 요구사항)")
    fun testCachePerformance() {
        // given: 설정 생성 (캐시에 데이터 준비)
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Performance", fieldName = "test", weight = 1.0)
        )

        // 첫 번째 조회 (캐시 미스 - DB에서 로딩)
        val cacheMissTime = measureTimeMillis {
            vectorConfigService.findByEntityType("Performance")
        }
        println("캐시 미스 (DB 조회): ${cacheMissTime}ms")

        // when: 두 번째 조회 (캐시 히트 - 메모리에서 로딩)
        val cacheHitTime = measureTimeMillis {
            vectorConfigService.findByEntityType("Performance")
        }
        println("캐시 히트 (메모리 조회): ${cacheHitTime}ms")

        // then: 성능 요구사항 검증
        assertThat(cacheHitTime).isLessThan(10) // 캐시 히트 < 10ms
        assertThat(cacheMissTime).isLessThan(100) // 캐시 미스 < 100ms

        // 추가: 캐시가 실제로 동작하는지 확인 (캐시 히트가 캐시 미스보다 빠름)
        assertThat(cacheHitTime).isLessThan(cacheMissTime)
        println("성능 개선: ${cacheMissTime - cacheHitTime}ms (${String.format("%.1f", (cacheMissTime.toDouble() / cacheHitTime) * 100)}% 빠름)")
    }

    @Test
    @DisplayName("캐시 성능 측정 - 특정 필드 조회 (캐시 히트 < 10ms)")
    fun testCachePerformanceByField() {
        // given
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "CacheTest", fieldName = "field1", weight = 1.0)
        )

        // 첫 번째 조회 (캐시 미스)
        val cacheMissTime = measureTimeMillis {
            vectorConfigService.findByEntityTypeAndFieldName("CacheTest", "field1")
        }

        // when: 두 번째 조회 (캐시 히트)
        val cacheHitTime = measureTimeMillis {
            vectorConfigService.findByEntityTypeAndFieldName("CacheTest", "field1")
        }

        // then
        assertThat(cacheHitTime).isLessThan(10) // 캐시 히트 < 10ms
        assertThat(cacheMissTime).isLessThan(100) // 캐시 미스 < 100ms
        println("findByEntityTypeAndFieldName - 캐시 히트: ${cacheHitTime}ms, 캐시 미스: ${cacheMissTime}ms")
    }

    @Test
    @DisplayName("캐시 성능 측정 - 여러 번 조회 시 일관된 성능 (캐시 안정성)")
    fun testCacheStability() {
        // given
        vectorConfigService.create(
            VectorConfigCreateRequest(entityType = "Stability", fieldName = "test", weight = 1.0)
        )

        // 첫 번째 조회 (캐시 미스)
        vectorConfigService.findByEntityType("Stability")

        // when: 10회 반복 조회 (모두 캐시 히트)
        val times = mutableListOf<Long>()
        repeat(10) {
            val time = measureTimeMillis {
                vectorConfigService.findByEntityType("Stability")
            }
            times.add(time)
        }

        // then: 모든 조회가 10ms 이내 (캐시 안정성 검증)
        assertThat(times).allMatch { it < 10 }
        println("10회 조회 평균: ${times.average()}ms, 최대: ${times.max()}ms, 최소: ${times.min()}ms")
    }
}
