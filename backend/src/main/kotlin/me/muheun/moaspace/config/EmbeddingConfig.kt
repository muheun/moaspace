package me.muheun.moaspace.config

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
 * MiniLM-L12-v2 ONNX 모델을 로딩하고 Spring Bean으로 관리합니다.
 * 애플리케이션 시작 시 모델을 한 번만 로딩하여 메모리에 상주시킵니다.
 *
 * 주요 기능:
 * - ONNX Runtime을 직접 사용한 모델 로딩
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

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

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
     * ONNX 세션 Bean 등록
     *
     * ONNX Runtime을 직접 사용하여 모델을 로딩합니다.
     *
     * @return OrtSession ONNX 세션 객체
     * @throws IOException 모델 파일 읽기 오류
     */
    @Bean
    fun onnxSession(env: OrtEnvironment): OrtSession {
        logger.info("ONNX 모델 로딩 시작: {}", modelPath)

        try {
            // 상대 경로를 절대 경로로 변환
            val absolutePath = when {
                modelPath.startsWith("/") -> Path.of(modelPath)
                modelPath.startsWith("./") -> Path.of(modelPath.substring(2)).toAbsolutePath()
                else -> Path.of(modelPath).toAbsolutePath()
            }

            logger.info("절대 경로로 변환: {}", absolutePath)

            // 파일 존재 확인
            if (!Files.exists(absolutePath)) {
                throw IOException("모델 파일을 찾을 수 없습니다: $absolutePath")
            }

            // 세션 옵션 설정
            val opts = OrtSession.SessionOptions()
            opts.setIntraOpNumThreads(4)
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            // 모델 바이트 읽기
            val modelBytes = Files.readAllBytes(absolutePath)
            logger.info("모델 파일 읽기 완료: {} MB", modelBytes.size / 1024 / 1024)

            // ONNX 세션 생성
            val session = env.createSession(modelBytes, opts)
            ortSession = session

            logger.info("✓ ONNX 모델 로딩 완료 (차원: {})", vectorDimension)

            return session

        } catch (e: Exception) {
            logger.error("✗ ONNX 모델 로딩 실패: {}", e.message, e)
            throw RuntimeException("ONNX 모델 로딩 실패: $modelPath", e)
        }
    }

    /**
     * Tokenizer 경로 Bean 등록
     *
     * HuggingFace Tokenizer의 tokenizer.json 파일 경로를 제공합니다.
     *
     * @return String tokenizer.json 파일 경로
     */
    @Bean
    fun tokenizerPath(): String {
        logger.info("Tokenizer 경로 설정: {}", tokenizerPath)
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
     * 애플리케이션 종료 시 ONNX Runtime 리소스를 명시적으로 해제합니다.
     * 메모리 누수 방지를 위해 반드시 호출되어야 합니다.
     */
    @PreDestroy
    fun cleanup() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
            logger.info("✓ ONNX Runtime 리소스 정리 완료")
        } catch (e: Exception) {
            logger.warn("ONNX Runtime 정리 중 오류 발생 (무시됨)", e)
        }
    }
}
