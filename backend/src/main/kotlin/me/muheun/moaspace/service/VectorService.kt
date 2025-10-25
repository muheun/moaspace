package me.muheun.moaspace.service

import com.pgvector.PGvector
import org.springframework.stereotype.Service
import kotlin.random.Random

/**
 * 벡터 임베딩 생성 서비스
 * 현재는 목업 벡터를 생성하지만, 추후 OpenAI API나 로컬 모델로 교체 가능
 */
@Service
class VectorService {

    companion object {
        const val VECTOR_DIMENSION = 1536 // OpenAI text-embedding-3-small과 호환되는 차원
    }

    /**
     * 텍스트를 벡터로 변환
     * 현재는 목업 데이터를 생성하지만, 실제로는 임베딩 모델을 사용해야 함
     *
     * @param text 벡터화할 텍스트
     * @return PGvector 객체
     */
    fun generateEmbedding(text: String): PGvector {
        // TODO: 실제 임베딩 모델로 교체 필요 (OpenAI API, sentence-transformers 등)
        // 현재는 텍스트 해시 기반 시드를 사용하여 일관된 목업 벡터 생성
        val seed = text.hashCode().toLong()
        val random = Random(seed)

        val vector = FloatArray(VECTOR_DIMENSION) {
            // -1.0 ~ 1.0 범위의 랜덤 값 생성
            random.nextFloat() * 2 - 1
        }

        // L2 정규화 (벡터의 크기를 1로 만듦)
        val norm = kotlin.math.sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())
        val normalizedVector = vector.map { it / norm }.toFloatArray()

        return PGvector(normalizedVector)
    }

    /**
     * 벡터를 문자열로 변환 (PostgreSQL 쿼리용)
     *
     * @param vector PGvector 객체
     * @return PostgreSQL vector 타입 문자열 형식
     */
    fun vectorToString(vector: PGvector): String {
        return vector.toString()
    }
}
