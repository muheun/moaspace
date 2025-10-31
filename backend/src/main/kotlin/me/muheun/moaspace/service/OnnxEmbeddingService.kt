package me.muheun.moaspace.service

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.pgvector.PGvector
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import kotlin.math.sqrt

private val logger = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

// ONNX Runtime 임베딩 서비스
@Service
@Primary
class OnnxEmbeddingService(
    private val ortEnvironment: OrtEnvironment,
    private val ortSession: OrtSession,
    private val tokenizer: HuggingFaceTokenizer,
    private val maxTokenLength: Int
) : VectorEmbeddingService {

    private val embeddingDimension = 768

    /**
     * 텍스트를 벡터로 변환
     *
     * 토큰화 → ONNX 추론 → Mean Pooling → L2 정규화 과정을 거쳐 768차원 벡터를 생성합니다.
     */
    override fun generateEmbedding(text: String): PGvector {
        require(text.isNotBlank()) { "입력 텍스트가 비어있습니다" }

        try {
            val encoding = tokenizer.encode(text)
            var inputIds = encoding.ids
            var attentionMask = encoding.attentionMask

            if (inputIds.size > maxTokenLength) {
                logger.warn("토큰 길이 초과 (${inputIds.size} > $maxTokenLength), truncate 수행")
                inputIds = inputIds.copyOf(maxTokenLength)
                attentionMask = attentionMask.copyOf(maxTokenLength)
            }

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
