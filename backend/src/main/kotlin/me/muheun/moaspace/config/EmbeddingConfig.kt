package me.muheun.moaspace.config

import ai.djl.MalformedModelException
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.IOException
import jakarta.annotation.PreDestroy
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * DJL 기반 ONNX 임베딩 모델 설정
 *
 * MiniLM-L12-v2 ONNX 모델을 로딩하고 Spring Bean으로 관리합니다.
 * 애플리케이션 시작 시 모델을 한 번만 로딩하여 메모리에 상주시킵니다.
 *
 * 주요 기능:
 * - ONNX Runtime Engine을 사용한 모델 로딩
 * - HuggingFace Tokenizer 통합
 * - 리소스 자동 정리 (@PreDestroy)
 */
@Configuration
class EmbeddingConfig {

    @Value("\${vector.embedding.model-path}")
    private lateinit var modelPath: String

    @Value("\${vector.embedding.tokenizer-path}")
    private lateinit var tokenizerPath: String

    @Value("\${vector.embedding.dimension:384}")
    private var vectorDimension: Int = 384

    @Value("\${vector.embedding.max-tokens:512}")
    private var maxTokens: Int = 512

    private var onnxModel: ZooModel<*, *>? = null

    /**
     * ONNX 모델 Bean 등록
     *
     * DJL Criteria.builder() 패턴을 사용하여 ONNX 모델을 로딩합니다.
     *
     * @return ZooModel ONNX 모델 객체
     * @throws MalformedModelException 모델 파일 형식 오류
     * @throws IOException 모델 파일 읽기 오류
     */
    @Bean
    fun onnxEmbeddingModel(): ZooModel<FloatArray, FloatArray> {
        logger.info { "DJL ONNX 모델 로딩 시작: $modelPath" }

        try {
            val criteria = Criteria.builder()
                .optEngine("OnnxRuntime") // ONNX Runtime Engine 명시적 설정
                .setTypes(FloatArray::class.java, FloatArray::class.java)
                .optModelPath(java.nio.file.Paths.get(modelPath))
                .optProgress(ProgressBar()) // 로딩 진행률 표시
                .build()

            val model = criteria.loadModel()
            onnxModel = model

            logger.info { "✓ ONNX 모델 로딩 완료 (차원: $vectorDimension)" }

            @Suppress("UNCHECKED_CAST")
            return model as ZooModel<FloatArray, FloatArray>

        } catch (e: Exception) {
            logger.error(e) { "✗ ONNX 모델 로딩 실패: ${e.message}" }
            throw RuntimeException("ONNX 모델 로딩 실패: $modelPath", e)
        }
    }

    /**
     * Tokenizer 경로 Bean 등록
     *
     * HuggingFace Tokenizer의 tokenizer.json 파일 경로를 제공합니다.
     * OnnxEmbeddingService에서 DJL HuggingFaceTokenizer를 사용할 때 참조합니다.
     *
     * @return String tokenizer.json 파일 경로
     */
    @Bean
    fun tokenizerPath(): String {
        logger.info { "Tokenizer 경로 설정: $tokenizerPath" }
        return tokenizerPath
    }

    /**
     * 벡터 차원 Bean 등록
     *
     * @return Int 벡터 차원 (MiniLM-L12-v2 = 384)
     */
    @Bean
    fun vectorDimension(): Int {
        return vectorDimension
    }

    /**
     * 최대 토큰 길이 Bean 등록
     *
     * @return Int 최대 토큰 길이 (기본값: 512)
     */
    @Bean
    fun maxTokenLength(): Int {
        return maxTokens
    }

    /**
     * 리소스 정리
     *
     * 애플리케이션 종료 시 DJL 모델 리소스를 명시적으로 해제합니다.
     * 메모리 누수 방지를 위해 반드시 호출되어야 합니다.
     */
    @PreDestroy
    fun cleanup() {
        try {
            onnxModel?.close()
            logger.info { "✓ ONNX 모델 리소스 정리 완료" }
        } catch (e: Exception) {
            logger.warn(e) { "모델 리소스 정리 중 오류 발생 (무시됨)" }
        }
    }
}
