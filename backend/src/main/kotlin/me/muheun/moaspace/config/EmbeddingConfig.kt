package me.muheun.moaspace.config

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.io.IOException
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
private val logger = LoggerFactory.getLogger(EmbeddingConfig::class.java)

/**
 * ONNX Runtime 기반 임베딩 모델 설정
 *
 * multilingual-e5-base ONNX 모델과 Tokenizer를 로딩하여 Spring Bean으로 관리합니다.
 * 애플리케이션 종료 시 리소스를 자동으로 정리합니다.
 *
 * **설정 검증**:
 * - @Validated: JSR-303 검증 활성화
 * - @PostConstruct: 애플리케이션 시작 시 설정값 유효성 검증
 */
@Configuration
@Validated
class EmbeddingConfig {

    /** OrtSession 참조 보관 (cleanup용) */
    private var sessionRef: OrtSession? = null

    /** OrtEnvironment 참조 보관 (cleanup용) */
    private var environmentRef: OrtEnvironment? = null

    /** HuggingFaceTokenizer 참조 보관 (cleanup용) */
    private var tokenizerRef: HuggingFaceTokenizer? = null

    /**
     * ONNX 모델 파일 경로
     *
     * **검증 규칙**:
     * - 비어있지 않아야 함 (@NotBlank)
     * - 실제 파일 존재 여부는 Bean 생성 시 확인
     */
    @Value("\${vector.embedding.model-path}")
    @NotBlank(message = "vector.embedding.model-path 설정이 비어있습니다")
    private lateinit var modelPath: String

    /**
     * HuggingFace Tokenizer 파일 경로
     *
     * **검증 규칙**:
     * - 비어있지 않아야 함 (@NotBlank)
     * - 실제 파일 존재 여부는 Bean 생성 시 확인
     */
    @Value("\${vector.embedding.tokenizer-path}")
    @NotBlank(message = "vector.embedding.tokenizer-path 설정이 비어있습니다")
    private lateinit var tokenizerPath: String

    /**
     * 벡터 임베딩 차원
     *
     * **검증 규칙**:
     * - 최소 64차원 ~ 최대 4096차원
     * - multilingual-e5-base 기본값: 768차원
     */
    @Value("\${vector.embedding.dimension:768}")
    @Min(value = 64, message = "vector.embedding.dimension은 최소 64 이상이어야 합니다")
    @Max(value = 4096, message = "vector.embedding.dimension은 최대 4096 이하여야 합니다")
    private var vectorDimension: Int = 768

    /**
     * 최대 토큰 길이
     *
     * **검증 규칙**:
     * - 최소 32 토큰 ~ 최대 8192 토큰
     * - multilingual-e5-base 기본값: 512 토큰
     */
    @Value("\${vector.embedding.max-tokens:512}")
    @Min(value = 32, message = "vector.embedding.max-tokens는 최소 32 이상이어야 합니다")
    @Max(value = 8192, message = "vector.embedding.max-tokens는 최대 8192 이하여야 합니다")
    private var maxTokens: Int = 512

    private var isClosed = false

    /**
     * 애플리케이션 시작 시 설정값 검증
     *
     * Bean 생성 전에 설정값의 유효성을 검증하여,
     * 잘못된 설정으로 인한 런타임 오류를 사전에 방지합니다.
     *
     * **검증 항목**:
     * 1. 모델 파일 경로가 올바른지 확인
     * 2. Tokenizer 파일 경로가 올바른지 확인
     * 3. 벡터 차원과 최대 토큰 길이가 유효 범위 내인지 확인
     */
    @PostConstruct
    fun validateConfiguration() {
        logger.info("임베딩 설정 검증 시작...")

        // 1. 모델 경로 검증
        require(modelPath.isNotBlank()) {
            "모델 경로가 비어있습니다. application.yml에서 vector.embedding.model-path를 설정하세요."
        }
        logger.info("✓ 모델 경로 설정: {}", modelPath)

        // 2. Tokenizer 경로 검증
        require(tokenizerPath.isNotBlank()) {
            "Tokenizer 경로가 비어있습니다. application.yml에서 vector.embedding.tokenizer-path를 설정하세요."
        }
        logger.info("✓ Tokenizer 경로 설정: {}", tokenizerPath)

        // 3. 벡터 차원 검증
        require(vectorDimension in 64..4096) {
            "벡터 차원이 유효 범위(64~4096)를 벗어났습니다: $vectorDimension"
        }
        logger.info("✓ 벡터 차원 설정: {}차원", vectorDimension)

        // 4. 최대 토큰 길이 검증
        require(maxTokens in 32..8192) {
            "최대 토큰 길이가 유효 범위(32~8192)를 벗어났습니다: $maxTokens"
        }
        logger.info("✓ 최대 토큰 길이 설정: {}토큰", maxTokens)

        logger.info("✓ 임베딩 설정 검증 완료")
    }

    /**
     * ONNX Runtime 환경 Bean 등록
     */
    @Bean
    fun ortEnvironment(): OrtEnvironment {
        val env = OrtEnvironment.getEnvironment()
        environmentRef = env
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
            tokenizerRef = tok

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
            sessionRef = session

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
     * 애플리케이션 종료 시 ONNX 리소스 정리
     *
     * OrtSession과 OrtEnvironment를 안전하게 정리하여 메모리 누수를 방지합니다.
     */
    @PreDestroy
    fun cleanup() {
        try {
            if (isClosed) {
                logger.debug("이미 정리된 리소스입니다. 건너뜁니다.")
                return
            }

            logger.info("ONNX 리소스 정리 시작...")

            // OrtSession 정리
            sessionRef?.let { session ->
                try {
                    session.close()
                    logger.info("✓ OrtSession 정리 완료")
                } catch (e: IllegalStateException) {
                    logger.debug("OrtSession이 이미 닫혀있습니다.")
                } catch (e: Exception) {
                    logger.error("✗ OrtSession 정리 실패: ${e.message}", e)
                }
            }

            // HuggingFaceTokenizer 정리
            tokenizerRef?.let { tokenizer ->
                try {
                    tokenizer.close()
                    logger.info("✓ HuggingFaceTokenizer 정리 완료")
                } catch (e: Exception) {
                    logger.error("✗ HuggingFaceTokenizer 정리 실패: ${e.message}", e)
                }
            }

            // OrtEnvironment 정리
            environmentRef?.let { env ->
                try {
                    env.close()
                    logger.info("✓ OrtEnvironment 정리 완료")
                } catch (e: Exception) {
                    logger.error("✗ OrtEnvironment 정리 실패: ${e.message}", e)
                }
            }

            isClosed = true
            logger.info("ONNX 리소스 정리 완료")

        } catch (e: Exception) {
            logger.error("ONNX 리소스 정리 중 오류 발생: ${e.message}", e)
        }
    }

}
