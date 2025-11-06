package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.vector.VectorConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VectorConfigRepositoryTest {

    @Autowired
    private lateinit var vectorConfigRepository: VectorConfigRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 vector_configs 테이블 정리
        entityManager.createNativeQuery("TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()
    }

    
    @Test
    @DisplayName("VectorConfig를 저장하고 조회한다")
    fun testSaveAndFindById() {
        // given
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0,
            threshold = 0.5,
            enabled = true
        )

        // when
        val saved = vectorConfigRepository.save(config)

        // then
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.entityType).isEqualTo("Post")
        assertThat(saved.fieldName).isEqualTo("title")
        assertThat(saved.weight).isEqualTo(2.0)
        assertThat(saved.threshold).isEqualTo(0.5)
        assertThat(saved.enabled).isTrue()

        // 조회 검증
        val found = vectorConfigRepository.findById(saved.id!!)
        assertThat(found).isPresent
        assertThat(found.get().entityType).isEqualTo("Post")
    }

    
    @Test
    @DisplayName("entityType으로 설정 목록을 조회한다")
    fun testFindByEntityType() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Comment", fieldName = "text", weight = 1.5)
        )

        // when
        val postConfigs = vectorConfigRepository.findByEntityType("Post")

        // then
        assertThat(postConfigs).hasSize(2)
        assertThat(postConfigs.map { it.fieldName })
            .containsExactlyInAnyOrder("title", "content")
    }

    
    @Test
    @DisplayName("entityType과 fieldName으로 설정을 조회한다")
    fun testFindByEntityTypeAndFieldNameExists() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )

        // when
        val config = vectorConfigRepository.findByEntityTypeAndFieldName("Post", "title")

        // then
        assertThat(config).isNotNull
        assertThat(config?.weight).isEqualTo(2.0)
    }

    
    @Test
    @DisplayName("존재하지 않는 설정 조회 시 null을 반환한다")
    fun testFindByEntityTypeAndFieldNameNotFound() {
        // when
        val config = vectorConfigRepository.findByEntityTypeAndFieldName("Post", "nonexistent")

        // then
        assertThat(config).isNull()
    }

    
    @Test
    @DisplayName("중복된 entityType과 fieldName 삽입 시 예외가 발생한다")
    fun testUniqueConstraintViolation() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )

        // when & then
        assertThrows<DataIntegrityViolationException> {
            vectorConfigRepository.saveAndFlush(
                VectorConfig(entityType = "Post", fieldName = "title", weight = 3.0)
            )
        }
    }

    
    @Test
    @DisplayName("VectorConfig 필드를 수정한다")
    fun testUpdate() {
        // given
        val config = vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, threshold = 0.0)
        )

        // when
        config.weight = 3.0
        config.threshold = 0.7
        config.enabled = false
        val updated = vectorConfigRepository.save(config)

        // then
        assertThat(updated.weight).isEqualTo(3.0)
        assertThat(updated.threshold).isEqualTo(0.7)
        assertThat(updated.enabled).isFalse()
    }

    
    @Test
    @DisplayName("VectorConfig를 삭제한다")
    fun testDelete() {
        // given
        val config = vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        val configId = config.id!!

        // when
        vectorConfigRepository.delete(config)

        // then
        val found = vectorConfigRepository.findById(configId)
        assertThat(found).isEmpty
    }

    
    @Test
    @DisplayName("모든 VectorConfig 설정을 조회한다")
    fun testFindAll() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0)
        )

        // when
        val allConfigs = vectorConfigRepository.findAll()

        // then
        assertThat(allConfigs).hasSizeGreaterThanOrEqualTo(2)
    }

    // ===========================
    // Phase 4: QueryDSL 동적 필터링 테스트 (T042-T045)
    // ===========================


    @Test
    @DisplayName("entityType 필터만 적용")
    fun testFindByFiltersEntityTypeOnly() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = false)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Comment", fieldName = "text", weight = 1.5, enabled = true)
        )

        // when
        val postConfigs = vectorConfigRepository.findByFilters(
            entityType = "Post",
            fieldName = null,
            enabled = null
        )

        // then
        assertThat(postConfigs).hasSize(2)
        assertThat(postConfigs.map { it.fieldName })
            .containsExactlyInAnyOrder("title", "content")
    }


    @Test
    @DisplayName("모든 파라미터 null 시 전체 조회")
    fun testFindByFiltersAllNull() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Comment", fieldName = "text", weight = 1.5)
        )

        // when
        val allConfigs = vectorConfigRepository.findByFilters(
            entityType = null,
            fieldName = null,
            enabled = null
        )

        // then
        assertThat(allConfigs).hasSizeGreaterThanOrEqualTo(2)
    }


    @Test
    @DisplayName("entityType + fieldName + enabled 결합")
    fun testFindByFiltersMultipleConditions() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = false)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Comment", fieldName = "text", weight = 1.5, enabled = true)
        )

        // when
        val filtered = vectorConfigRepository.findByFilters(
            entityType = "Post",
            fieldName = "title",
            enabled = true
        )

        // then
        assertThat(filtered).hasSize(1)
        assertThat(filtered.first().entityType).isEqualTo("Post")
        assertThat(filtered.first().fieldName).isEqualTo("title")
        assertThat(filtered.first().enabled).isTrue()
    }
}
