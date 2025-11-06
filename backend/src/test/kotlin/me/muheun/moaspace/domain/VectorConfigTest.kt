package me.muheun.moaspace.domain

import me.muheun.moaspace.domain.vector.VectorConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * VectorConfig 엔티티 단위 테스트
 */
class VectorConfigTest {

    @Test
    @DisplayName("VectorConfig 엔티티를 생성한다")
    fun testCreateVectorConfig() {
        // given
        val entityType = "Post"
        val fieldName = "title"
        val weight = 2.0
        val threshold = 0.5
        val enabled = true

        // when
        val config = VectorConfig(
            entityType = entityType,
            fieldName = fieldName,
            weight = weight,
            threshold = threshold,
            enabled = enabled
        )

        // then
        assertThat(config.entityType).isEqualTo(entityType)
        assertThat(config.fieldName).isEqualTo(fieldName)
        assertThat(config.weight).isEqualTo(weight)
        assertThat(config.threshold).isEqualTo(threshold)
        assertThat(config.enabled).isEqualTo(enabled)
        assertThat(config.createdAt).isBeforeOrEqualTo(LocalDateTime.now())
        assertThat(config.updatedAt).isBeforeOrEqualTo(LocalDateTime.now())
    }

    @Test
    @DisplayName("VectorConfig 기본값이 올바르게 설정된다")
    fun testDefaultValues() {
        // given & when
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "content"
        )

        // then
        assertThat(config.weight).isEqualTo(1.0)
        assertThat(config.threshold).isEqualTo(0.0)
        assertThat(config.enabled).isTrue()
    }

    @Test
    @DisplayName("preUpdate 훅이 updatedAt을 갱신한다")
    fun testPreUpdateHook() {
        // given
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "title",
            weight = 1.0
        )
        val originalUpdatedAt = config.updatedAt

        // when
        Thread.sleep(10) // 시간 차이를 위해 대기
        config.preUpdate()

        // then
        assertThat(config.updatedAt).isAfter(originalUpdatedAt)
    }

    @Test
    @DisplayName("VectorConfig 가중치를 수정한다")
    fun testModifyWeight() {
        // given
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "title",
            weight = 1.0
        )

        // when
        config.weight = 2.5

        // then
        assertThat(config.weight).isEqualTo(2.5)
    }

    @Test
    @DisplayName("VectorConfig 임계값을 수정한다")
    fun testModifyThreshold() {
        // given
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "title",
            threshold = 0.0
        )

        // when
        config.threshold = 0.7

        // then
        assertThat(config.threshold).isEqualTo(0.7)
    }

    @Test
    @DisplayName("VectorConfig 활성화 여부를 토글한다")
    fun testToggleEnabled() {
        // given
        val config = VectorConfig(
            entityType = "Post",
            fieldName = "title",
            enabled = true
        )

        // when
        config.enabled = false

        // then
        assertThat(config.enabled).isFalse()
    }
}
