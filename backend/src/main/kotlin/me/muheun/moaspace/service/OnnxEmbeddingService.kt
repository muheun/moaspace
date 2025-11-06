package me.muheun.moaspace.service

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Semaphore
import kotlin.math.sqrt

private val logger = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

/**
 * ONNX Runtime 기반 임베딩 서비스
 *
 * multilingual-e5-base ONNX 모델과 Tokenizer를 직접 관리합니다.
 * @PostConstruct에서 리소스를 초기화하고 @PreDestroy에서 정리합니다.
 *
 * Semaphore를 사용하여 제한된 병렬 처리를 지원합니다.
 * 동시 실행 개수를 제한하여 Thread-safety를 보장하면서도 성능을 향상시킵니다.
 */
@Service
@Primary
class OnnxEmbeddingService : VectorEmbeddingService {

    @Value("\${vector.embedding.model-path}")
    private lateinit var modelPath: String

    @Value("\${vector.embedding.tokenizer-path}")
    private lateinit var tokenizerPath: String

    @Value("\${vector.embedding.dimension:768}")
    private var embeddingDimension: Int = 768

    @Value("\${vector.embedding.max-tokens:512}")
    private var maxTokenLength: Int = 512

    @Value("\${vector.embedding.max-concurrent:4}")
    private var maxConcurrent: Int = 4

    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var tokenizer: HuggingFaceTokenizer
    private lateinit var semaphore: Semaphore

    @PostConstruct
    fun init() {
        logger.info("ONNX 임베딩 서비스 초기화 시작...")

        try {
            // OrtEnvironment 초기화
            ortEnvironment = OrtEnvironment.getEnvironment()
            logger.info("✓ ONNX Runtime 환경 초기화 완료")

            // HuggingFace Tokenizer 로딩
            val tokenizerAbsolutePath = resolvePath(tokenizerPath)
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerAbsolutePath)
            logger.info("✓ HuggingFace Tokenizer 로딩 완료: {}", tokenizerAbsolutePath)

            // ONNX 세션 생성
            val modelAbsolutePath = resolvePath(modelPath)
            val opts = OrtSession.SessionOptions()
            opts.setIntraOpNumThreads(4)
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            val modelBytes = Files.readAllBytes(modelAbsolutePath)
            ortSession = ortEnvironment.createSession(modelBytes, opts)
            logger.info("✓ ONNX 모델 로딩 완료: {} ({} MB)", modelAbsolutePath, modelBytes.size / 1024 / 1024)

            // Semaphore 초기화 (제한된 병렬 처리)
            semaphore = Semaphore(maxConcurrent, true) // fair=true로 FIFO 보장
            logger.info("✓ Semaphore 초기화 완료 (최대 동시 실행: {})", maxConcurrent)

            logger.info("✓ ONNX 임베딩 서비스 초기화 완료 (차원: {}, 최대 토큰: {}, 동시 실행: {})",
                embeddingDimension, maxTokenLength, maxConcurrent)

        } catch (e: Exception) {
            logger.error("✗ ONNX 임베딩 서비스 초기화 실패: {}", e.message, e)
            throw RuntimeException("ONNX 임베딩 서비스 초기화 실패", e)
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            logger.info("ONNX 리소스 정리 시작...")

            ortSession.close()
            logger.info("✓ OrtSession 정리 완료")

            tokenizer.close()
            logger.info("✓ HuggingFaceTokenizer 정리 완료")

            ortEnvironment.close()
            logger.info("✓ OrtEnvironment 정리 완료")

            logger.info("✓ ONNX 리소스 정리 완료")

        } catch (e: Exception) {
            logger.error("ONNX 리소스 정리 중 오류 발생: ${e.message}", e)
        }
    }

    private fun resolvePath(path: String): Path {
        return when {
            path.startsWith("/") -> Path.of(path)
            path.startsWith("./") -> Path.of(path.substring(2)).toAbsolutePath()
            else -> Path.of(path).toAbsolutePath()
        }
    }

    /**
     * 텍스트를 벡터로 변환
     *
     * 토큰화 → ONNX 추론 → Mean Pooling → L2 정규화 과정을 거쳐 768차원 벡터를 생성합니다.
     *
     * Semaphore를 사용하여 제한된 병렬 처리를 지원합니다.
     * 최대 maxConcurrent개의 스레드가 동시에 실행 가능하며, 나머지는 대기합니다.
     */
    override fun generateEmbedding(text: String): PGvector {
        require(text.isNotBlank()) { "입력 텍스트가 비어있습니다" }

        // Semaphore permit 획득 (대기 가능)
        semaphore.acquire()

        try {
            // 토큰화
            val encoding = tokenizer.encode(text)
            var inputIds = encoding.ids
            var attentionMask = encoding.attentionMask

            if (inputIds.size > maxTokenLength) {
                logger.warn("토큰 길이 초과 (${inputIds.size} > $maxTokenLength), truncate 수행")
                inputIds = inputIds.copyOf(maxTokenLength)
                attentionMask = attentionMask.copyOf(maxTokenLength)
            }

            // ONNX 추론
            val batchInputIds = arrayOf(inputIds)
            val batchAttentionMask = arrayOf(attentionMask)

            val inputIdsTensor = OnnxTensor.createTensor(ortEnvironment, batchInputIds)
            val attentionMaskTensor = OnnxTensor.createTensor(ortEnvironment, batchAttentionMask)

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            val result = ortSession.run(inputs)

            try {
                val outputValue = result.get(0)
                val outputTensor = outputValue as OnnxTensor
                @Suppress("UNCHECKED_CAST")
                val outputArray = outputTensor.value as Array<Array<FloatArray>>

                val actualDim = if (outputArray.isNotEmpty() && outputArray[0].isNotEmpty()) {
                    outputArray[0][0].size
                } else 0
                logger.warn("⚠️ ONNX 모델 실제 출력 차원: $actualDim (설정: $embeddingDimension)")

                val embedding = meanPooling(outputArray[0], attentionMask)
                normalizeL2(embedding)

                logger.debug("임베딩 생성 완료: 텍스트 길이=${text.length}, 벡터 차원=${embedding.size}")

                return PGvector(embedding)

            } finally {
                result.close()
                inputIdsTensor.close()
                attentionMaskTensor.close()
            }

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            logger.error("임베딩 생성 중 오류 발생: ${e.message}", e)
            throw RuntimeException("임베딩 생성 실패", e)
        } finally {
            // Semaphore permit 반환 (항상 실행)
            semaphore.release()
        }
    }

    /**
     * Attention mask를 고려한 평균 풀링으로 문장 임베딩 생성
     */
    private fun meanPooling(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val result = FloatArray(embeddingDimension)
        var validTokens = 0

        val maxLength = minOf(tokenEmbeddings.size, attentionMask.size)
        for (i in 0 until maxLength) {
            if (attentionMask[i] == 1L) {
                for (j in 0 until embeddingDimension) {
                    result[j] += tokenEmbeddings[i][j]
                }
                validTokens++
            }
        }

        if (validTokens > 0) {
            for (j in 0 until embeddingDimension) {
                result[j] = result[j] / validTokens
            }
        }

        return result
    }

    /**
     * L2 정규화로 벡터의 크기를 1.0으로 만들어 코사인 유사도 계산에 최적화
     */
    private fun normalizeL2(vector: FloatArray) {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())

        if (norm < 1e-12f) {
            logger.warn("Zero vector 감지, 정규화 불가")
            throw IllegalStateException("벡터 norm이 0입니다 (zero vector)")
        }

        for (i in vector.indices) {
            vector[i] /= norm
        }

        logger.debug("L2 정규화 완료: norm=$norm → 1.0")
    }
}
