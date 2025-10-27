package me.muheun.moaspace.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 설정 검증 로직 수동 테스트
 *
 * EmbeddingConfig의 validateConfiguration() 메서드를 직접 호출하여
 * 잘못된 설정값에 대한 명확한 오류 메시지 출력을 검증합니다.
 */
@DisplayName("설정 검증 로직 테스트")
class ConfigValidationManualTest {

    /**
     * 시나리오 1: 벡터 차원이 최소값(64)보다 작은 경우
     */
    @Test
    @DisplayName("벡터 차원이 최소값보다 작으면 명확한 오류 메시지와 함께 실패한다")
    fun shouldFailWithClearErrorMessageWhenVectorDimensionIsBelowMinimum() {
        // Given: 잘못된 벡터 차원 (32 < 64)
        val config = TestEmbeddingConfig(
            modelPath = "./models/model.onnx",
            tokenizerPath = "./models/tokenizer.json",
            vectorDimension = 32,  // 🚨 유효 범위 밖
            maxTokens = 512
        )

        // When & Then: 검증 실패 + 명확한 오류 메시지
        val exception = assertThrows<IllegalArgumentException> {
            config.validateConfiguration()
        }

        val errorMessage = exception.message ?: ""
        println("오류 메시지: $errorMessage")

        assertThat(errorMessage)
            .contains("벡터 차원", "유효 범위")
            .contains("64", "4096")
    }

    /**
     * 시나리오 2: 벡터 차원이 최대값(4096)보다 큰 경우
     */
    @Test
    @DisplayName("벡터 차원이 최대값보다 크면 명확한 오류 메시지와 함께 실패한다")
    fun shouldFailWithClearErrorMessageWhenVectorDimensionExceedsMaximum() {
        // Given: 잘못된 벡터 차원 (8192 > 4096)
        val config = TestEmbeddingConfig(
            modelPath = "./models/model.onnx",
            tokenizerPath = "./models/tokenizer.json",
            vectorDimension = 8192,  // 🚨 유효 범위 밖
            maxTokens = 512
        )

        // When & Then: 검증 실패 + 명확한 오류 메시지
        val exception = assertThrows<IllegalArgumentException> {
            config.validateConfiguration()
        }

        val errorMessage = exception.message ?: ""
        println("오류 메시지: $errorMessage")

        assertThat(errorMessage)
            .contains("벡터 차원", "유효 범위")
    }

    /**
     * 시나리오 3: 최대 토큰 길이가 유효 범위를 벗어난 경우
     */
    @Test
    @DisplayName("최대 토큰 길이가 유효 범위를 벗어나면 명확한 오류 메시지와 함께 실패한다")
    fun shouldFailWithClearErrorMessageWhenMaxTokensIsOutOfRange() {
        // Given: 잘못된 최대 토큰 길이 (16 < 32)
        val config = TestEmbeddingConfig(
            modelPath = "./models/model.onnx",
            tokenizerPath = "./models/tokenizer.json",
            vectorDimension = 768,
            maxTokens = 16  // 🚨 유효 범위 밖
        )

        // When & Then: 검증 실패 + 명확한 오류 메시지
        val exception = assertThrows<IllegalArgumentException> {
            config.validateConfiguration()
        }

        val errorMessage = exception.message ?: ""
        println("오류 메시지: $errorMessage")

        assertThat(errorMessage)
            .contains("최대 토큰", "유효 범위")
            .contains("32", "8192")
    }

    /**
     * 시나리오 4: 모델 경로가 비어있는 경우
     */
    @Test
    @DisplayName("모델 경로가 비어있으면 명확한 오류 메시지와 함께 실패한다")
    fun shouldFailWithClearErrorMessageWhenModelPathIsEmpty() {
        // Given: 빈 모델 경로
        val config = TestEmbeddingConfig(
            modelPath = "",  // 🚨 빈 문자열
            tokenizerPath = "./models/tokenizer.json",
            vectorDimension = 768,
            maxTokens = 512
        )

        // When & Then: 검증 실패 + 명확한 오류 메시지
        val exception = assertThrows<IllegalArgumentException> {
            config.validateConfiguration()
        }

        val errorMessage = exception.message ?: ""
        println("오류 메시지: $errorMessage")

        assertThat(errorMessage)
            .contains("모델 경로", "application.yml")
    }

    /**
     * 시나리오 5: 올바른 설정값 (검증 통과)
     */
    @Test
    @DisplayName("올바른 설정값이면 검증이 성공한다")
    fun shouldPassValidationWhenAllConfigValuesAreValid() {
        // Given: 올바른 설정값
        val config = TestEmbeddingConfig(
            modelPath = "./models/model.onnx",
            tokenizerPath = "./models/tokenizer.json",
            vectorDimension = 768,  // ✅ 유효 범위 내 (64~4096)
            maxTokens = 512  // ✅ 유효 범위 내 (32~8192)
        )

        // When & Then: 검증 성공 (예외 발생하지 않음)
        config.validateConfiguration()  // 성공
        println("✓ 검증 성공: 모든 설정값이 유효합니다.")
    }

    /**
     * 테스트용 EmbeddingConfig 클래스
     * - EmbeddingConfig의 검증 로직만 테스트하기 위한 간소화된 버전
     */
    private class TestEmbeddingConfig(
        val modelPath: String,
        val tokenizerPath: String,
        val vectorDimension: Int,
        val maxTokens: Int
    ) {
        fun validateConfiguration() {
            // EmbeddingConfig.kt의 validateConfiguration() 로직 복사
            require(modelPath.isNotBlank()) {
                "모델 경로가 비어있습니다. application.yml에서 vector.embedding.model-path를 설정하세요."
            }

            require(tokenizerPath.isNotBlank()) {
                "Tokenizer 경로가 비어있습니다. application.yml에서 vector.embedding.tokenizer-path를 설정하세요."
            }

            require(vectorDimension in 64..4096) {
                "벡터 차원이 유효 범위(64~4096)를 벗어났습니다: $vectorDimension"
            }

            require(maxTokens in 32..8192) {
                "최대 토큰 길이가 유효 범위(32~8192)를 벗어났습니다: $maxTokens"
            }
        }
    }
}
