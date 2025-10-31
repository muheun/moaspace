package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorConfig
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

/**
 * VectorConfigRepository 통합 테스트
 *
 * @SpringBootTest로 실제 DB 연동 테스트 수행
 * Constitution Principle V 준수: Real Database Integration + @Transactional rollback
 */
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

    /**
     * VectorConfig 저장 및 조회 테스트
     *
     * Given: VectorConfig 엔티티 생성
     * When: save() 호출 후 findById()로 조회
     * Then: 저장된 데이터 정상 조회 및 모든 필드값 일치
     */
    @Test
    @DisplayName("testSaveAndFindById - VectorConfig를 저장하고 조회한다")
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

    /**
     * findByEntityType 테스트
     *
     * Given: Post(2개), Comment(1개) 설정 저장
     * When: findByEntityType("Post") 호출
     * Then: Post 타입 설정 2개만 조회
     */
    @Test
    @DisplayName("testFindByEntityType - entityType으로 설정 목록을 조회한다")
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

    /**
     * findByEntityTypeAndFieldName 테스트 - 존재하는 경우
     *
     * Given: Post.title 설정 저장
     * When: findByEntityTypeAndFieldName("Post", "title") 호출
     * Then: 해당 설정 조회 성공
     */
    @Test
    @DisplayName("testFindByEntityTypeAndFieldNameExists - entityType과 fieldName으로 설정을 조회한다")
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

    /**
     * findByEntityTypeAndFieldName 테스트 - 존재하지 않는 경우
     *
     * Given: 설정 없음
     * When: 존재하지 않는 fieldName으로 조회
     * Then: null 반환
     */
    @Test
    @DisplayName("testFindByEntityTypeAndFieldNameNotFound - 존재하지 않는 설정 조회 시 null을 반환한다")
    fun testFindByEntityTypeAndFieldNameNotFound() {
        // when
        val config = vectorConfigRepository.findByEntityTypeAndFieldName("Post", "nonexistent")

        // then
        assertThat(config).isNull()
    }

    /**
     * 복합 유니크 키 제약 테스트 - 중복 삽입 실패
     *
     * Given: Post.title 설정 이미 존재
     * When: 동일한 entityType + fieldName으로 재삽입 시도
     * Then: DataIntegrityViolationException 발생 (UNIQUE 제약)
     */
    @Test
    @DisplayName("testUniqueConstraintViolation - 중복된 entityType과 fieldName 삽입 시 예외가 발생한다")
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

    /**
     * VectorConfig 수정 테스트
     *
     * Given: 기존 설정 저장
     * When: weight, threshold, enabled 필드 수정 후 save()
     * Then: 수정된 값으로 업데이트 성공
     */
    @Test
    @DisplayName("testUpdate - VectorConfig 필드를 수정한다")
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

    /**
     * VectorConfig 삭제 테스트
     *
     * Given: 기존 설정 저장
     * When: delete() 호출
     * Then: 조회 시 Empty 반환 (물리적 삭제)
     */
    @Test
    @DisplayName("testDelete - VectorConfig를 삭제한다")
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

    /**
     * 모든 VectorConfig 조회 테스트
     *
     * Given: 여러 설정 저장
     * When: findAll() 호출
     * Then: 모든 설정 조회 (최소 2개 이상)
     */
    @Test
    @DisplayName("testFindAll - 모든 VectorConfig 설정을 조회한다")
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
}
