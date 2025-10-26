package me.muheun.moaspace.config

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

private val logger = LoggerFactory.getLogger(EmbeddingConfig::class.java)

/**
 * ONNX Runtime 기반 임베딩 모델 설정
 *
 * multilingual-e5-base ONNX 모델과 Tokenizer를 로딩하여 Spring Bean으로 관리합니다.
 * 애플리케이션 종료 시 리소스를 자동으로 정리합니다.
 */
@Configuration
class EmbeddingConfig {

    @Value("\${vector.embedding.model-path}")
    private lateinit var modelPath: String

    @Value("\${vector.embedding.tokenizer-path}")
    private lateinit var tokenizerPath: String

    @Value("\${vector.embedding.dimension:768}")
    private var vectorDimension: Int = 768

    @Value("\${vector.embedding.max-tokens:512}")
    private var maxTokens: Int = 512

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: HuggingFaceTokenizer? = null

    /**
     * ONNX Runtime 환경 Bean 등록
     */
    @Bean
    fun ortEnvironment(): OrtEnvironment {
        val env = OrtEnvironment.getEnvironment()
        ortEnvironment = env
        logger.info("✓ ONNX Runtime 환경 초기화 완료")
        return env
    }

    /**
     * HuggingFace Tokenizer Bean 등록
     */
    @Bean
    fun huggingFaceTokenizer(): HuggingFaceTokenizer {
        logger.info("HuggingFace Tokenizer 로딩 시작: {}", tokenizerPath)

        try {
            val absolutePath = when {
                tokenizerPath.startsWith("/") -> Path.of(tokenizerPath)
                tokenizerPath.startsWith("./") -> Path.of(tokenizerPath.substring(2)).toAbsolutePath()
                else -> Path.of(tokenizerPath).toAbsolutePath()
            }

            logger.info("Tokenizer 절대 경로: {}", absolutePath)

            if (!Files.exists(absolutePath)) {
                throw IOException("Tokenizer 파일을 찾을 수 없습니다: $absolutePath")
            }

            val tok = HuggingFaceTokenizer.newInstance(absolutePath)
            tokenizer = tok

            logger.info("✓ HuggingFace Tokenizer 로딩 완료")

            return tok

        } catch (e: Exception) {
            logger.error("✗ HuggingFace Tokenizer 로딩 실패: {}", e.message, e)
            throw RuntimeException("HuggingFace Tokenizer 로딩 실패: $tokenizerPath", e)
        }
    }

    /**
     * ONNX 세션 Bean 등록
     */
    @Bean
    fun onnxSession(env: OrtEnvironment): OrtSession {
        logger.info("ONNX 모델 로딩 시작: {}", modelPath)

        try {
            val absolutePath = when {
                modelPath.startsWith("/") -> Path.of(modelPath)
                modelPath.startsWith("./") -> Path.of(modelPath.substring(2)).toAbsolutePath()
                else -> Path.of(modelPath).toAbsolutePath()
            }

            logger.info("절대 경로로 변환: {}", absolutePath)

            if (!Files.exists(absolutePath)) {
                throw IOException("모델 파일을 찾을 수 없습니다: $absolutePath")
            }

            val opts = OrtSession.SessionOptions()
            opts.setIntraOpNumThreads(4)
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            val modelBytes = Files.readAllBytes(absolutePath)
            logger.info("모델 파일 읽기 완료: {} MB", modelBytes.size / 1024 / 1024)

            val session = env.createSession(modelBytes, opts)
            ortSession = session

            val outputInfo = session.outputInfo
            outputInfo.forEach { (name, info) ->
                logger.warn("⚠️ ONNX 모델 출력: name=$name, info=${info.info}")
            }

            logger.info("✓ ONNX 모델 로딩 완료 (설정 차원: {})", vectorDimension)

            return session

        } catch (e: Exception) {
            logger.error("✗ ONNX 모델 로딩 실패: {}", e.message, e)
            throw RuntimeException("ONNX 모델 로딩 실패: $modelPath", e)
        }
    }

    /**
     * Tokenizer 경로 Bean 등록
     */
    @Bean
    fun tokenizerPath(): String {
        logger.info("Tokenizer 경로 설정: {}", tokenizerPath)
        return tokenizerPath
    }

    /**
     * 벡터 차원 Bean 등록
     */
    @Bean
    fun vectorDimension(): Int {
        return vectorDimension
    }

    /**
     * 최대 토큰 길이 Bean 등록
     */
    @Bean
    fun maxTokenLength(): Int {
        return maxTokens
    }

    /**
     * 리소스 정리
     *
     * 애플리케이션 종료 시 ONNX Runtime 리소스를 명시적으로 해제합니다.
     */
    @PreDestroy
    fun cleanup() {
        try {
            tokenizer?.close()
            ortSession?.close()
            ortEnvironment?.close()
            logger.info("✓ ONNX Runtime 및 Tokenizer 리소스 정리 완료")
        } catch (e: Exception) {
            logger.warn("ONNX Runtime 정리 중 오류 발생 (무시됨)", e)
        }
    }
}
