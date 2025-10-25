package me.muheun.moaspace.service

import ai.djl.huggingface.tokenizers.Encoding
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.math.sqrt

private val logger = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

/**
 * ONNX Runtime 기반 임베딩 서비스
 *
 * multilingual-e5-base ONNX 모델을 사용하여 실제 의미 기반 벡터 임베딩을 생성합니다.
 *
 * 주요 기능:
 * - HuggingFace Tokenizer를 사용한 토큰화
 * - ONNX Runtime을 통한 모델 추론
 * - Mean Pooling으로 문장 임베딩 생성
 * - L2 정규화로 코사인 유사도 최적화
 * - 100개 언어 지원 (다국어 최적화)
 *
 * @property ortSession ONNX Runtime 세션
 * @property tokenizerPath HuggingFace Tokenizer 경로
 * @property maxTokenLength 최대 토큰 길이 (512)
 */
@Service
@Primary
class OnnxEmbeddingService(
    private val ortEnvironment: ai.onnxruntime.OrtEnvironment,
    private val ortSession: OrtSession,
    @Qualifier("tokenizerPath") private val tokenizerPath: String,
    @Qualifier("maxTokenLength") private val maxTokenLength: Int
) : VectorEmbeddingService {

    private lateinit var tokenizer: HuggingFaceTokenizer
    private val embeddingDimension = 768 // multilingual-e5-base (100개 언어 지원)

    @PostConstruct
    fun init() {
        try {
            // Tokenizer 초기화
            val tempTokenizerPath = Files.createTempFile("tokenizer", ".json")
            tempTokenizerPath.toFile().deleteOnExit()

            val tokenizerFile = Path.of(tokenizerPath)
            if (!Files.exists(tokenizerFile)) {
                throw RuntimeException("Tokenizer 파일을 찾을 수 없습니다: $tokenizerPath")
            }

            Files.copy(tokenizerFile, tempTokenizerPath, StandardCopyOption.REPLACE_EXISTING)
            tokenizer = HuggingFaceTokenizer.newInstance(tempTokenizerPath)

            logger.info("✓ HuggingFace Tokenizer 로딩 완료")
            logger.info("✓ ONNX 임베딩 서비스 초기화 완료")

        } catch (e: Exception) {
            logger.error("✗ ONNX 임베딩 서비스 초기화 실패: ${e.message}", e)
            throw RuntimeException("ONNX 임베딩 서비스 초기화 실패", e)
        }
    }

    /**
     * 텍스트를 벡터로 변환
     *
     * 전체 파이프라인:
     * 1. 입력 검증
     * 2. HuggingFace Tokenizer로 토큰화
     * 3. ONNX Runtime 추론
     * 4. Mean Pooling
     * 5. L2 정규화
     *
     * @param text 벡터화할 텍스트
     * @return PGvector 객체 (768차원, L2 정규화됨)
     * @throws IllegalArgumentException 빈 문자열 또는 null 입력
     * @throws RuntimeException ONNX 추론 실패
     */
    override fun generateEmbedding(text: String): PGvector {
        // 1. 입력 검증
        require(text.isNotBlank()) { "입력 텍스트가 비어있습니다" }

        try {
            // 2. 토큰화
            val encoding: Encoding = tokenizer.encode(text)
            var inputIds = encoding.ids
            var attentionMask = encoding.attentionMask

            // Truncate 처리
            if (inputIds.size > maxTokenLength) {
                logger.warn("토큰 길이 초과 (${inputIds.size} > $maxTokenLength), truncate 수행")
                inputIds = inputIds.copyOf(maxTokenLength)
                attentionMask = attentionMask.copyOf(maxTokenLength)
            }

            // 배치 차원 추가 [1, seq_len]
            val batchInputIds = arrayOf(inputIds)
            val batchAttentionMask = arrayOf(attentionMask)

            // 3. ONNX 텐서 생성
            val inputIdsTensor = OnnxTensor.createTensor(ortEnvironment, batchInputIds)
            val attentionMaskTensor = OnnxTensor.createTensor(ortEnvironment, batchAttentionMask)

            // 4. ONNX 추론
            // E5는 XLM-RoBERTa 기반 아키텍처이므로 token_type_ids가 필요 없음
            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            val result = ortSession.run(inputs)

            try {
                // 출력 추출 (last_hidden_state: [batch_size, seq_len, hidden_size])
                val outputValue = result.get(0)
                val outputTensor = outputValue as OnnxTensor
                @Suppress("UNCHECKED_CAST")
                val outputArray = outputTensor.value as Array<Array<FloatArray>>

                // 5. Mean Pooling
                val embedding = meanPooling(outputArray[0], attentionMask)

                // 6. L2 정규화
                normalizeL2(embedding)

                logger.debug("임베딩 생성 완료: 텍스트 길이=${text.length}, 벡터 차원=${embedding.size}")

                return PGvector(embedding)

            } finally {
                // 리소스 정리
                result.close()
                inputIdsTensor.close()
                attentionMaskTensor.close()
            }

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            logger.error("임베딩 생성 중 오류 발생: ${e.message}", e)
            throw RuntimeException("임베딩 생성 실패", e)
        }
    }

    /**
     * Mean pooling - attention mask를 고려한 평균
     *
     * @param tokenEmbeddings 토큰별 임베딩 [seq_len, hidden_size]
     * @param attentionMask attention mask [seq_len]
     * @return 평균 임베딩 [hidden_size]
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
     * L2 정규화
     *
     * 벡터의 L2 norm을 1.0으로 만들어 코사인 유사도 계산을 최적화합니다.
     *
     * @param vector 원본 벡터 (in-place 수정됨)
     * @throws IllegalStateException norm이 0인 경우
     */
    private fun normalizeL2(vector: FloatArray) {
        // L2 norm 계산
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())

        // Zero vector 처리
        if (norm < 1e-12f) {
            logger.warn("Zero vector 감지, 정규화 불가")
            throw IllegalStateException("벡터 norm이 0입니다 (zero vector)")
        }

        // 정규화 (in-place)
        for (i in vector.indices) {
            vector[i] /= norm
        }

        logger.debug("L2 정규화 완료: norm=$norm → 1.0")
    }
}
