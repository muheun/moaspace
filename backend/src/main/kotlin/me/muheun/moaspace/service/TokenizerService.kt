package me.muheun.moaspace.service

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.ModelType
import org.springframework.stereotype.Service

/**
 * OpenAI 토크나이저 서비스
 *
 * jtokkit 라이브러리를 사용하여 텍스트를 토큰으로 변환하고 토큰 수를 계산합니다.
 * 이를 통해 OpenAI API의 토큰 제한에 맞춰 정확한 청크 크기를 계산할 수 있습니다.
 *
 * 주요 기능:
 * - 텍스트 -> 토큰 ID 리스트 변환
 * - 토큰 수 계산
 * - 토큰 ID 리스트 -> 텍스트 복원
 *
 * @see <a href="https://github.com/knuddelsgmbh/jtokkit">jtokkit GitHub</a>
 */
@Service
class TokenizerService {

    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()

    /**
     * OpenAI text-embedding-3-small 모델용 인코딩
     * cl100k_base 인코딩 사용 (GPT-4, GPT-3.5-turbo, text-embedding-3 공통)
     */
    private val encoding: Encoding = registry.getEncodingForModel(ModelType.TEXT_EMBEDDING_3_SMALL)

    /**
     * 텍스트를 토큰 ID 리스트로 변환
     *
     * @param text 변환할 텍스트
     * @return 토큰 ID 리스트
     */
    fun encode(text: String): List<Int> {
        val tokenIds = encoding.encode(text)
        return (0 until tokenIds.size()).map { tokenIds[it] }
    }

    /**
     * 토큰 ID 리스트를 텍스트로 복원
     *
     * @param tokens 토큰 ID 리스트
     * @return 복원된 텍스트
     */
    fun decode(tokens: List<Int>): String {
        val tokenIds = com.knuddels.jtokkit.api.IntArrayList()
        tokens.forEach { tokenIds.add(it) }
        return encoding.decode(tokenIds)
    }

    /**
     * 텍스트의 토큰 수 계산
     *
     * @param text 토큰 수를 계산할 텍스트
     * @return 토큰 수
     */
    fun countTokens(text: String): Int {
        return encoding.countTokens(text)
    }

    /**
     * 텍스트를 최대 토큰 수에 맞춰 잘라냄
     *
     * 토큰 수가 maxTokens를 초과하면 maxTokens까지만 유지하고 나머지는 버림
     *
     * @param text 자를 텍스트
     * @param maxTokens 최대 토큰 수
     * @return 잘라낸 텍스트
     */
    fun truncateToTokenLimit(text: String, maxTokens: Int): String {
        val tokens = encode(text)
        if (tokens.size <= maxTokens) {
            return text
        }

        val truncatedTokens = tokens.take(maxTokens)
        return decode(truncatedTokens)
    }
}
