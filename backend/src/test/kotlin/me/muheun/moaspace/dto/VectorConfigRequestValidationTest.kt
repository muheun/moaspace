package me.muheun.moaspace.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * VectorConfig Request DTO Bean Validation 테스트
 *
 * Jakarta Bean Validation 어노테이션이 올바르게 동작하는지 검증
 */
class VectorConfigRequestValidationTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    // ========== VectorConfigCreateRequest 검증 ==========

    @Test
    @DisplayName("모든 필드 유효한 경우 검증 통과")
    fun testCreateRequestValid() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 2.0,
            threshold = 0.5,
            enabled = true
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("entityType이 빈 문자열인 경우 검증 실패")
    fun testCreateRequestEmptyEntityType() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "",
            fieldName = "title",
            weight = 1.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("entityType")
    }

    @Test
    @DisplayName("fieldName이 빈 문자열인 경우 검증 실패")
    fun testCreateRequestEmptyFieldName() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "",
            weight = 1.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("fieldName")
    }

    @Test
    @DisplayName("entityType이 100자 초과인 경우 검증 실패")
    fun testCreateRequestEntityTypeTooLong() {
        // given
        val longEntityType = "A".repeat(101)
        val request = VectorConfigCreateRequest(
            entityType = longEntityType,
            fieldName = "title",
            weight = 1.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("entityType")
    }

    @Test
    @DisplayName("fieldName이 100자 초과인 경우 검증 실패")
    fun testCreateRequestFieldNameTooLong() {
        // given
        val longFieldName = "A".repeat(101)
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = longFieldName,
            weight = 1.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("fieldName")
    }

    @Test
    @DisplayName("weight가 0.1 미만인 경우 검증 실패")
    fun testCreateRequestWeightTooLow() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 0.05
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("weight")
        assertThat(violations.first { it.propertyPath.toString() == "weight" }.message)
            .contains("0.1 이상")
    }

    @Test
    @DisplayName("weight가 10.0 초과인 경우 검증 실패")
    fun testCreateRequestWeightTooHigh() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 15.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("weight")
        assertThat(violations.first { it.propertyPath.toString() == "weight" }.message)
            .contains("10.0 이하")
    }

    @Test
    @DisplayName("threshold가 0.0 미만인 경우 검증 실패")
    fun testCreateRequestThresholdTooLow() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 1.0,
            threshold = -0.1
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("threshold")
        assertThat(violations.first { it.propertyPath.toString() == "threshold" }.message)
            .contains("0.0 이상")
    }

    @Test
    @DisplayName("threshold가 1.0 초과인 경우 검증 실패")
    fun testCreateRequestThresholdTooHigh() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 1.0,
            threshold = 1.5
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("threshold")
        assertThat(violations.first { it.propertyPath.toString() == "threshold" }.message)
            .contains("1.0 이하")
    }

    @Test
    @DisplayName("경계값 테스트 (weight=0.1, threshold=0.0)")
    fun testCreateRequestBoundaryValuesLow() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 0.1,
            threshold = 0.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("경계값 테스트 (weight=10.0, threshold=1.0)")
    fun testCreateRequestBoundaryValuesHigh() {
        // given
        val request = VectorConfigCreateRequest(
            entityType = "Post",
            fieldName = "title",
            weight = 10.0,
            threshold = 1.0
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    // ========== VectorConfigUpdateRequest 검증 ==========

    @Test
    @DisplayName("모든 필드 null인 경우도 검증 통과 (부분 업데이트)")
    fun testUpdateRequestAllNull() {
        // given
        val request = VectorConfigUpdateRequest()

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("weight만 수정하는 경우 검증 통과")
    fun testUpdateRequestOnlyWeight() {
        // given
        val request = VectorConfigUpdateRequest(weight = 3.5)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("threshold만 수정하는 경우 검증 통과")
    fun testUpdateRequestOnlyThreshold() {
        // given
        val request = VectorConfigUpdateRequest(threshold = 0.7)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("enabled만 수정하는 경우 검증 통과")
    fun testUpdateRequestOnlyEnabled() {
        // given
        val request = VectorConfigUpdateRequest(enabled = false)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("weight가 0.1 미만인 경우 검증 실패")
    fun testUpdateRequestWeightTooLow() {
        // given
        val request = VectorConfigUpdateRequest(weight = 0.05)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("weight")
        assertThat(violations.first { it.propertyPath.toString() == "weight" }.message)
            .contains("0.1 이상")
    }

    @Test
    @DisplayName("weight가 10.0 초과인 경우 검증 실패")
    fun testUpdateRequestWeightTooHigh() {
        // given
        val request = VectorConfigUpdateRequest(weight = 15.0)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("weight")
        assertThat(violations.first { it.propertyPath.toString() == "weight" }.message)
            .contains("10.0 이하")
    }

    @Test
    @DisplayName("threshold가 0.0 미만인 경우 검증 실패")
    fun testUpdateRequestThresholdTooLow() {
        // given
        val request = VectorConfigUpdateRequest(threshold = -0.1)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("threshold")
        assertThat(violations.first { it.propertyPath.toString() == "threshold" }.message)
            .contains("0.0 이상")
    }

    @Test
    @DisplayName("threshold가 1.0 초과인 경우 검증 실패")
    fun testUpdateRequestThresholdTooHigh() {
        // given
        val request = VectorConfigUpdateRequest(threshold = 1.5)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("threshold")
        assertThat(violations.first { it.propertyPath.toString() == "threshold" }.message)
            .contains("1.0 이하")
    }

    @Test
    @DisplayName("경계값 테스트 (weight=0.1)")
    fun testUpdateRequestBoundaryWeightLow() {
        // given
        val request = VectorConfigUpdateRequest(weight = 0.1)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("경계값 테스트 (weight=10.0)")
    fun testUpdateRequestBoundaryWeightHigh() {
        // given
        val request = VectorConfigUpdateRequest(weight = 10.0)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("경계값 테스트 (threshold=0.0)")
    fun testUpdateRequestBoundaryThresholdLow() {
        // given
        val request = VectorConfigUpdateRequest(threshold = 0.0)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("경계값 테스트 (threshold=1.0)")
    fun testUpdateRequestBoundaryThresholdHigh() {
        // given
        val request = VectorConfigUpdateRequest(threshold = 1.0)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("여러 필드 동시 수정 시 모두 유효한 경우 검증 통과")
    fun testUpdateRequestMultipleFieldsValid() {
        // given
        val request = VectorConfigUpdateRequest(
            weight = 5.0,
            threshold = 0.8,
            enabled = false
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    @DisplayName("여러 필드 중 하나라도 유효하지 않으면 검증 실패")
    fun testUpdateRequestMultipleFieldsInvalid() {
        // given
        val request = VectorConfigUpdateRequest(
            weight = 15.0, // 유효하지 않음
            threshold = 0.5 // 유효함
        )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.map { it.propertyPath.toString() }).contains("weight")
    }
}
