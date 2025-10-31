package me.muheun.moaspace.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.muheun.moaspace.dto.VectorConfigCreateRequest
import me.muheun.moaspace.dto.VectorConfigUpdateRequest
import me.muheun.moaspace.repository.VectorConfigRepository
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

/**
 * VectorConfigController REST API 통합 테스트
 *
 * Constitution Principle V 준수: Real Database Integration
 * - @SpringBootTest + MockMvc로 전체 HTTP 요청/응답 검증
 * - 실제 DB 사용 (Mock 금지)
 * - @Transactional로 각 테스트 격리 및 롤백
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class VectorConfigControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
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
    @DisplayName("POST /api/vector-configs - 새로운 벡터 설정 생성 (201 Created)")
    fun testCreateVectorConfig() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0,
            threshold = 0.5,
            enabled = true
        )

        // when & then
        mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id", greaterThan(0)))
            .andExpect(jsonPath("$.entityType").value("Post"))
            .andExpect(jsonPath("$.fieldName").value("title"))
            .andExpect(jsonPath("$.weight").value(2.0))
            .andExpect(jsonPath("$.threshold").value(0.5))
            .andExpect(jsonPath("$.enabled").value(true))
    }

    @Test
    @DisplayName("POST /api/vector-configs - 중복 생성 시 409 Conflict")
    fun testCreateDuplicateVectorConfig() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0
        )

        // 첫 번째 생성 성공
        mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // when & then: 중복 생성 시도 (409 Conflict 기대)
        mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 존재하는 설정입니다")))
    }

    @Test
    @DisplayName("POST /api/vector-configs - Bean Validation 실패 (400 Bad Request)")
    fun testCreateWithInvalidWeight() {
        // given: 가중치 범위 초과 (0.1 ~ 10.0)
        val request = mapOf(
            "entityType" to "Post",
            "fieldName" to "title",
            "weight" to 15.0 // 유효하지 않은 값
        )

        // when & then
        mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/vector-configs - 모든 벡터 설정 조회 (200 OK)")
    fun testFindAllVectorConfigs() {
        // given: 2개 설정 생성
        vectorConfigRepository.deleteAll()
        createConfig("Post", "title", 2.0)
        createConfig("Post", "content", 1.0)

        // when & then
        mockMvc.perform(get("/api/vector-configs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[0].entityType").value("Post"))
    }

    @Test
    @DisplayName("GET /api/vector-configs/{id} - ID로 설정 조회 (200 OK)")
    fun testFindVectorConfigById() {
        // given
        val configId = createConfig("Post", "title", 2.0)

        // when & then
        mockMvc.perform(get("/api/vector-configs/$configId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(configId))
            .andExpect(jsonPath("$.entityType").value("Post"))
            .andExpect(jsonPath("$.fieldName").value("title"))
    }

    @Test
    @DisplayName("GET /api/vector-configs/{id} - 존재하지 않는 ID 조회 (404 Not Found)")
    fun testFindVectorConfigByIdNotFound() {
        // when & then
        mockMvc.perform(get("/api/vector-configs/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /api/vector-configs/entity/{entityType}/field/{fieldName} - 설정 조회 (200 OK)")
    fun testFindVectorConfigByEntityTypeAndFieldName() {
        // given
        createConfig("Post", "title", 2.0)

        // when & then
        mockMvc.perform(get("/api/vector-configs/entity/Post/field/title"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entityType").value("Post"))
            .andExpect(jsonPath("$.fieldName").value("title"))
            .andExpect(jsonPath("$.weight").value(2.0))
    }

    @Test
    @DisplayName("GET /api/vector-configs/entity/{entityType}/field/{fieldName} - 존재하지 않는 설정 (404 Not Found)")
    fun testFindVectorConfigByEntityTypeAndFieldNameNotFound() {
        // when & then
        mockMvc.perform(get("/api/vector-configs/entity/Post/field/nonexistent"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("설정 생성 → 조회 → DB 저장 확인 (User Story 1 시나리오)")
    fun testUserStory1Scenario() {
        // Step 1: 설정 생성
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0
        )

        val result = mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseJson = objectMapper.readTree(result.response.contentAsString)
        val configId = responseJson.get("id").asLong()

        // Step 2: 설정 조회
        mockMvc.perform(get("/api/vector-configs/$configId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(configId))
            .andExpect(jsonPath("$.weight").value(2.0))

        // Step 3: DB 저장 확인
        val saved = vectorConfigRepository.findById(configId)
        assert(saved.isPresent)
        assert(saved.get().weight == 2.0)
    }

    @Test
    @DisplayName("PUT /api/vector-configs/{id} - 벡터 설정 수정 (200 OK)")
    fun testUpdateVectorConfig() {
        // given
        val configId = createConfig("Post", "title", 2.0)

        val updateRequest = VectorConfigUpdateRequest(
            weight = 3.5,
            threshold = 0.7,
            enabled = false
        )

        // when & then
        mockMvc.perform(
            put("/api/vector-configs/$configId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(configId))
            .andExpect(jsonPath("$.weight").value(3.5))
            .andExpect(jsonPath("$.threshold").value(0.7))
            .andExpect(jsonPath("$.enabled").value(false))

        // DB 저장 확인
        val updated = vectorConfigRepository.findById(configId).get()
        assert(updated.weight == 3.5)
        assert(updated.threshold == 0.7)
        assert(!updated.enabled)
    }

    @Test
    @DisplayName("PUT /api/vector-configs/{id} - 일부 필드만 수정 (200 OK)")
    fun testUpdateVectorConfigPartial() {
        // given
        val configId = createConfig("Post", "content", 1.0)

        val updateRequest = VectorConfigUpdateRequest(weight = 2.5)

        // when & then
        mockMvc.perform(
            put("/api/vector-configs/$configId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weight").value(2.5))
            .andExpect(jsonPath("$.threshold").value(0.0)) // 변경되지 않음
            .andExpect(jsonPath("$.enabled").value(true)) // 변경되지 않음
    }

    @Test
    @DisplayName("PUT /api/vector-configs/{id} - 존재하지 않는 설정 수정 (404 Not Found)")
    fun testUpdateVectorConfigNotFound() {
        // given
        val updateRequest = VectorConfigUpdateRequest(weight = 3.0)

        // when & then
        mockMvc.perform(
            put("/api/vector-configs/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("PUT /api/vector-configs/{id} - Bean Validation 실패 (400 Bad Request)")
    fun testUpdateVectorConfigInvalidWeight() {
        // given
        val configId = createConfig("Post", "tags", 1.0)

        val updateRequest = mapOf(
            "weight" to 15.0 // 유효하지 않은 값 (0.1 ~ 10.0 범위 초과)
        )

        // when & then
        mockMvc.perform(
            put("/api/vector-configs/$configId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("DELETE /api/vector-configs/{id} - 벡터 설정 삭제 (204 No Content)")
    fun testDeleteVectorConfig() {
        // given
        val configId = createConfig("Post", "description", 1.5)

        // when & then
        mockMvc.perform(delete("/api/vector-configs/$configId"))
            .andExpect(status().isNoContent)

        // DB에서 삭제 확인
        val deleted = vectorConfigRepository.findById(configId)
        assert(deleted.isEmpty)
    }

    @Test
    @DisplayName("DELETE /api/vector-configs/{id} - 존재하지 않는 설정 삭제 (404 Not Found)")
    fun testDeleteVectorConfigNotFound() {
        // when & then
        mockMvc.perform(delete("/api/vector-configs/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("설정 수정 → 조회 → 삭제 → 조회 (404) (User Story 2 시나리오)")
    fun testUserStory2Scenario() {
        // Step 1: 설정 생성
        val configId = createConfig("Post", "subtitle", 2.0)

        // Step 2: 설정 수정
        val updateRequest = VectorConfigUpdateRequest(weight = 3.0, threshold = 0.6)
        mockMvc.perform(
            put("/api/vector-configs/$configId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weight").value(3.0))
            .andExpect(jsonPath("$.threshold").value(0.6))

        // Step 3: 수정된 설정 조회
        mockMvc.perform(get("/api/vector-configs/$configId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weight").value(3.0))

        // Step 4: 설정 삭제
        mockMvc.perform(delete("/api/vector-configs/$configId"))
            .andExpect(status().isNoContent)

        // Step 5: 삭제된 설정 조회 시도 (404)
        mockMvc.perform(get("/api/vector-configs/$configId"))
            .andExpect(status().isNotFound)

        // DB에서 삭제 확인
        val deleted = vectorConfigRepository.findById(configId)
        assert(deleted.isEmpty)
    }

    /**
     * Helper: 설정 생성 및 ID 반환
     */
    private fun createConfig(entityType: String, fieldName: String, weight: Double): Long {
        val request = VectorConfigCreateRequest(
            entityType = entityType,
            fieldName = fieldName,
            weight = weight
        )

        val result = mockMvc.perform(
            post("/api/vector-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseJson = objectMapper.readTree(result.response.contentAsString)
        return responseJson.get("id").asLong()
    }
}
