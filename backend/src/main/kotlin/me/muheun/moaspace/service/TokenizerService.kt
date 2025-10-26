package me.muheun.moaspace.service

import ai.djl.huggingface.tokenizers.Encoding
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * HuggingFace 토크나이저 서비스
 *
 * multilingual-e5-base 모델의 토크나이저를 사용하여 텍스트를 인코딩하고 토큰 개수를 계산합니다.
 */
@Service
class TokenizerService @Autowired constructor(
    private val tokenizer: HuggingFaceTokenizer,
    @Value("\${vector.embedding.max-tokens:512}") private val maxTokens: Int
) {

    /**
     * 텍스트를 토큰 ID 리스트로 인코딩
     */
    fun encode(text: String): List<Long> {
        if (text.isEmpty()) return emptyList()

        val encoding = tokenizer.encode(text)
        return encoding.ids.toList()
    }

    /**
     * 토큰 ID 리스트를 텍스트로 디코딩
     */
    fun decode(tokens: List<Long>): String {
        if (tokens.isEmpty()) return ""

        return tokenizer.decode(tokens.toLongArray())
    }

    /**
     * 텍스트의 실제 토큰 수 계산
     */
    fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0

        val encoding = tokenizer.encode(text)
        return encoding.ids.size
    }

    /**
     * 텍스트를 최대 토큰 수에 맞춰 잘라냄
     *
     * 토큰 단위로 정확하게 자르고 다시 텍스트로 디코딩합니다.
     */
    fun truncateToTokenLimit(text: String, maxTokens: Int): String {
        if (text.isEmpty()) return ""

        val encoding = tokenizer.encode(text)
        val tokenIds = encoding.ids

        if (tokenIds.size <= maxTokens) {
            return text
        }

        val truncatedTokens = tokenIds.copyOfRange(0, maxTokens)
        return tokenizer.decode(truncatedTokens)
    }

    /**
     * 텍스트를 인코딩하고 Encoding 객체 반환 (내부 사용)
     */
    fun encodeWithDetails(text: String): Encoding {
        return tokenizer.encode(text)
    }
}
