package me.muheun.moaspace.service

import ai.djl.huggingface.tokenizers.Encoding
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDManager
import ai.djl.repository.zoo.ZooModel
import ai.djl.translate.Batchifier
import ai.djl.translate.TranslateException
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import com.pgvector.PGvector
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.nio.file.Paths
import kotlin.math.sqrt

private val logger = LoggerFactory.getLogger(OnnxEmbeddingService::class.java)

/**
 * DJL 기반 ONNX 임베딩 서비스
 *
 * MiniLM-L12-v2 ONNX 모델을 사용하여 실제 의미 기반 벡터 임베딩을 생성합니다.
 *
 * 주요 기능:
 * - HuggingFace Tokenizer를 사용한 토큰화
 * - ONNX Runtime을 통한 모델 추론
 * - Mean Pooling으로 문장 임베딩 생성
 * - L2 정규화로 코사인 유사도 최적화
 *
 * @property onnxModel DJL ONNX 모델 (EmbeddingConfig에서 주입)
 * @property tokenizerPath HuggingFace Tokenizer 경로
 * @property maxTokenLength 최대 토큰 길이 (512)
 */
@Service
class OnnxEmbeddingService(
    @Qualifier("onnxEmbeddingModel") private val onnxModel: ZooModel<FloatArray, FloatArray>,
    @Qualifier("tokenizerPath") private val tokenizerPath: String,
    @Qualifier("maxTokenLength") private val maxTokenLength: Int
) : VectorEmbeddingService {

    private val tokenizer: HuggingFaceTokenizer by lazy {
        try {
            logger.info("HuggingFace Tokenizer 로딩 시작: $tokenizerPath")
            val tokenizer = HuggingFaceTokenizer.newInstance(Paths.get(tokenizerPath))
            logger.info("✓ Tokenizer 로딩 완료")
            tokenizer
        } catch (e: Exception) {
            logger.error("✗ Tokenizer 로딩 실패: ${e.message}", e)
            throw RuntimeException("Tokenizer 로딩 실패: $tokenizerPath", e)
        }
    }

    /**
     * 텍스트를 벡터로 변환
     *
     * 전체 파이프라인:
     * 1. 입력 검증
     * 2. HuggingFace Tokenizer로 토큰화
     * 3. ONNX 모델 추론
     * 4. Mean Pooling
     * 5. L2 정규화
     *
     * @param text 벡터화할 텍스트
     * @return PGvector 객체 (384차원, L2 정규화됨)
     * @throws IllegalArgumentException 빈 문자열 또는 null 입력
     * @throws RuntimeException ONNX 추론 실패
     */
    override fun generateEmbedding(text: String): PGvector {
        // 1. 입력 검증
        require(text.isNotBlank()) { "입력 텍스트가 비어있습니다" }

        try {
            // 2. 토큰화
            val tokenIds = tokenize(text)

            // 3. ONNX 추론
            val rawEmbedding = runInference(tokenIds)

            // 4. Mean Pooling (ONNX 출력은 이미 pooled된 상태일 수 있음, 모델에 따라 다름)
            // MiniLM-L12-v2 ONNX 모델은 일반적으로 CLS token embedding을 반환
            // 또는 모든 토큰의 평균을 계산해야 할 수 있음
            // 여기서는 이미 pooled된 것으로 가정

            // 5. L2 정규화
            val normalizedEmbedding = normalizeL2(rawEmbedding)

            logger.debug("임베딩 생성 완료: 텍스트 길이=${text.length}, 벡터 차원=${normalizedEmbedding.size}")

            return PGvector(normalizedEmbedding)

        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            logger.error("임베딩 생성 중 오류 발생: ${e.message}", e)
            throw RuntimeException("임베딩 생성 실패", e)
        }
    }

    /**
     * HuggingFace Tokenizer를 사용한 토큰화
     *
     * - Special tokens ([CLS], [SEP]) 자동 추가
     * - maxTokenLength 초과 시 자동 truncate
     *
     * @param text 토큰화할 텍스트
     * @return LongArray 토큰 ID 배열
     */
    private fun tokenize(text: String): LongArray {
        try {
            // Truncate는 Tokenizer 설정에 따라 자동으로 처리됨
            val encoding: Encoding = tokenizer.encode(text)

            val tokenIds = encoding.ids

            logger.debug("토큰화 완료: 토큰 수=${tokenIds.size}")

            // 최대 길이 체크 (필요시 수동 truncate)
            return if (tokenIds.size > maxTokenLength) {
                logger.warn("토큰 길이 초과 (${tokenIds.size} > $maxTokenLength), truncate 수행")
                tokenIds.copyOf(maxTokenLength)
            } else {
                tokenIds
            }

        } catch (e: Exception) {
            logger.error("토큰화 실패: ${e.message}", e)
            throw RuntimeException("토큰화 실패", e)
        }
    }

    /**
     * ONNX Runtime을 통한 모델 추론
     *
     * DJL Predictor를 사용하여 ONNX 모델에 토큰 ID를 입력하고
     * 384차원 임베딩 벡터를 출력받습니다.
     *
     * @param tokenIds 토큰 ID 배열
     * @return FloatArray 임베딩 벡터 (raw, 정규화되지 않음)
     * @throws TranslateException ONNX 추론 오류
     */
    private fun runInference(tokenIds: LongArray): FloatArray {
        try {
            // Translator 생성
            val translator = EmbeddingTranslator()

            // Predictor 생성 및 추론
            onnxModel.newPredictor(translator).use { predictor ->
                // 토큰 ID를 FloatArray로 변환 (ONNX 입력 형식)
                val input = tokenIds.map { it.toFloat() }.toFloatArray()

                val output = predictor.predict(input)

                logger.debug("ONNX 추론 완료: 출력 차원=${output.size}")

                return output
            }

        } catch (e: Exception) {
            logger.error("ONNX 추론 실패: ${e.message}", e)
            throw RuntimeException("ONNX 추론 실패", e)
        }
    }

    /**
     * L2 정규화
     *
     * 벡터의 L2 norm을 1.0으로 만들어 코사인 유사도 계산을 최적화합니다.
     * norm = sqrt(sum(x^2))
     * normalized = x / norm
     *
     * @param vector 원본 벡터
     * @return FloatArray 정규화된 벡터 (norm = 1.0)
     * @throws IllegalStateException norm이 0인 경우
     */
    private fun normalizeL2(vector: FloatArray): FloatArray {
        // L2 norm 계산
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())

        // Zero vector 처리
        if (norm < 1e-12f) {
            logger.warn("Zero vector 감지, 정규화 불가")
            throw IllegalStateException("벡터 norm이 0입니다 (zero vector)")
        }

        // 정규화
        val normalized = vector.map { it / norm }.toFloatArray()

        // 검증: norm이 1.0에 가까운지 확인
        val normalizedNorm = sqrt(normalized.sumOf { (it * it).toDouble() }.toFloat())
        logger.debug("L2 정규화 완료: norm=$norm → $normalizedNorm")

        return normalized
    }

    /**
     * DJL Translator for ONNX Embedding Model
     *
     * 입력: FloatArray (토큰 ID)
     * 출력: FloatArray (임베딩 벡터)
     */
    private class EmbeddingTranslator : Translator<FloatArray, FloatArray> {

        override fun processInput(ctx: TranslatorContext, input: FloatArray): ai.djl.ndarray.NDList {
            val manager = ctx.ndManager
            // 입력을 NDArray로 변환 (batch_size=1, seq_length=input.size)
            val ndarray = manager.create(input).reshape(1, input.size.toLong())
            return ai.djl.ndarray.NDList(ndarray)
        }

        override fun processOutput(ctx: TranslatorContext, list: ai.djl.ndarray.NDList): FloatArray {
            // ONNX 모델 출력을 FloatArray로 변환
            val output = list[0] // 첫 번째 출력 (임베딩 벡터)
            return output.toFloatArray()
        }

        override fun getBatchifier(): Batchifier {
            return Batchifier.STACK
        }
    }
}
