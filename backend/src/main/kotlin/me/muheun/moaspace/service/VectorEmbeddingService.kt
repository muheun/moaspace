package me.muheun.moaspace.service

import com.pgvector.PGvector

/**
 * 벡터 임베딩 생성 인터페이스
 *
 * 다양한 임베딩 제공자(ONNX, OpenAI, Mock 등)를 추상화합니다.
 * 이 인터페이스를 구현하면 임베딩 제공자를 쉽게 교체할 수 있습니다.
 */
interface VectorEmbeddingService {

    /**
     * 텍스트를 벡터로 변환
     *
     * @param text 벡터화할 텍스트
     * @return PGvector 객체 (L2 정규화된 벡터)
     * @throws IllegalArgumentException 빈 문자열 또는 null 입력
     */
    fun generateEmbedding(text: String): PGvector
}
