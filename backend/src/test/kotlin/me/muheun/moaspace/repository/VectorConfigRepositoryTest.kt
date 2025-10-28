package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.VectorConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.jdbc.Sql

/**
 * VectorConfigRepository 통합 테스트
 *
 * @DataJpaTest로 실제 DB 연동 테스트 수행
 * Constitution Principle V 준수: Real Database Integration + @Sql 초기화
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class VectorConfigRepositoryTest {

    @Autowired
    private lateinit var vectorConfigRepository: VectorConfigRepository

    @Test
    fun `VectorConfig 저장 및 조회 테스트`() {
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
    fun `findByEntityType 테스트`() {
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
    fun `findByEntityTypeAndFieldName 테스트 - 존재하는 경우`() {
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
    fun `findByEntityTypeAndFieldName 테스트 - 존재하지 않는 경우`() {
        // when
        val config = vectorConfigRepository.findByEntityTypeAndFieldName("Post", "nonexistent")

        // then
        assertThat(config).isNull()
    }

    @Test
    fun `복합 유니크 키 제약 테스트 - 중복 삽입 실패`() {
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
    fun `VectorConfig 수정 테스트`() {
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
    fun `VectorConfig 삭제 테스트`() {
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
    fun `모든 VectorConfig 조회 테스트`() {
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
