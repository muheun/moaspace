package me.muheun.moaspace.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * VectorConfig 엔티티 단위 테스트
 */
class VectorConfigTest {

    @Test
    fun `VectorConfig 엔티티 생성 테스트`() {
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
    fun `VectorConfig 기본값 테스트`() {
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
    fun `VectorConfig preUpdate 훅 테스트`() {
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
    fun `VectorConfig 가중치 수정 테스트`() {
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
    fun `VectorConfig 임계값 수정 테스트`() {
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
    fun `VectorConfig 활성화 여부 토글 테스트`() {
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
