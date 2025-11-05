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

    /**
     * VectorConfig 엔티티 생성 테스트
     *
     * Given: entityType, fieldName, weight, threshold, enabled 값
     * When: VectorConfig 생성자 호출
     * Then: 모든 필드 정상 설정 + createdAt/updatedAt 자동 생성
     */
    @Test
    @DisplayName("testCreateVectorConfig - VectorConfig 엔티티를 생성한다")
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

    /**
     * VectorConfig 기본값 테스트
     *
     * Given: entityType, fieldName만 제공
     * When: VectorConfig 생성
     * Then: weight=1.0, threshold=0.0, enabled=true 기본값 설정
     */
    @Test
    @DisplayName("testDefaultValues - VectorConfig 기본값이 올바르게 설정된다")
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

    /**
     * VectorConfig preUpdate 훅 테스트
     *
     * Given: VectorConfig 엔티티 생성
     * When: preUpdate() 호출
     * Then: updatedAt 타임스탬프 갱신
     */
    @Test
    @DisplayName("testPreUpdateHook - preUpdate 훅이 updatedAt을 갱신한다")
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

    /**
     * VectorConfig 가중치 수정 테스트
     *
     * Given: weight=1.0 설정
     * When: weight를 2.5로 변경
     * Then: 변경된 값 반영
     */
    @Test
    @DisplayName("testModifyWeight - VectorConfig 가중치를 수정한다")
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

    /**
     * VectorConfig 임계값 수정 테스트
     *
     * Given: threshold=0.0 설정
     * When: threshold를 0.7로 변경
     * Then: 변경된 값 반영
     */
    @Test
    @DisplayName("testModifyThreshold - VectorConfig 임계값을 수정한다")
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

    /**
     * VectorConfig 활성화 여부 토글 테스트
     *
     * Given: enabled=true 설정
     * When: enabled를 false로 변경
     * Then: 변경된 값 반영
     */
    @Test
    @DisplayName("testToggleEnabled - VectorConfig 활성화 여부를 토글한다")
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
