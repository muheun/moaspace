package me.muheun.moaspace.service

import org.springframework.stereotype.Service

/**
 * 텍스트 청킹 서비스
 * 긴 문서를 적절한 크기의 청크로 분할하여 벡터 임베딩 품질 향상
 */
@Service
class ChunkingService {

    companion object {
        // 청킹 기준 (문자 수 기반)
        private const val MIN_CHUNK_SIZE = 200        // 최소 청크 크기
        private const val TARGET_CHUNK_SIZE = 500     // 목표 청크 크기
        private const val MAX_CHUNK_SIZE = 1000       // 최대 청크 크기
        private const val OVERLAP_SIZE = 100          // 청크 간 중복 크기

        // 문서 길이 임계값 (이보다 짧으면 청킹하지 않음)
        private const val MIN_DOCUMENT_LENGTH = 300
    }

    /**
     * 텍스트를 청크로 분할
     *
     * 전략:
     * 1. 짧은 문서 (300자 미만): 청크 생성 안 함 (전체를 하나의 청크로)
     * 2. 중간 문서: 문단 기반 분할
     * 3. 긴 문서: 적응형 크기 조정 + 오버래핑
     *
     * @param text 분할할 순수 텍스트
     * @return TextChunk 리스트
     */
    fun chunkDocument(text: String): List<TextChunk> {
        val cleanedText = text.trim()

        // 빈 텍스트 처리
        if (cleanedText.isEmpty()) {
            return emptyList()
        }

        // 짧은 문서는 청킹하지 않음
        if (cleanedText.length < MIN_DOCUMENT_LENGTH) {
            return listOf(
                TextChunk(
                    text = cleanedText,
                    index = 0,
                    startPos = 0,
                    endPos = cleanedText.length
                )
            )
        }

        // 문단 기반 분할 시도
        val paragraphs = splitIntoParagraphs(cleanedText)

        // 청크 생성
        return createChunksFromParagraphs(paragraphs, cleanedText)
    }

    /**
     * 텍스트를 문단으로 분할
     *
     * 분할 기준:
     * - \n\n (빈 줄)
     * - 최소 3개 이상의 연속된 공백
     */
    private fun splitIntoParagraphs(text: String): List<String> {
        return text
            .split(Regex("\n\\s*\n|\\s{3,}"))  // 빈 줄 또는 연속된 공백
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 문단들을 적절한 크기의 청크로 그룹화
     */
    private fun createChunksFromParagraphs(paragraphs: List<String>, originalText: String): List<TextChunk> {
        val chunks = mutableListOf<TextChunk>()
        var currentChunk = StringBuilder()
        var currentStartPos = 0
        var chunkIndex = 0

        paragraphs.forEach { paragraph ->
            val paragraphLength = paragraph.length

            // 현재 청크가 비어있으면 문단 추가
            if (currentChunk.isEmpty()) {
                currentChunk.append(paragraph)
                currentStartPos = originalText.indexOf(paragraph, currentStartPos)
            }
            // 문단을 추가해도 최대 크기를 넘지 않으면 추가
            else if (currentChunk.length + paragraphLength + 1 <= MAX_CHUNK_SIZE) {
                currentChunk.append("\n").append(paragraph)
            }
            // 현재 청크가 목표 크기 이상이면 청크 완성
            else if (currentChunk.length >= TARGET_CHUNK_SIZE) {
                // 현재 청크 저장
                val chunkText = currentChunk.toString()
                val endPos = currentStartPos + chunkText.length

                chunks.add(
                    TextChunk(
                        text = chunkText,
                        index = chunkIndex++,
                        startPos = currentStartPos,
                        endPos = endPos
                    )
                )

                // 오버래핑을 위한 처리
                val overlap = getOverlapText(chunkText)
                currentChunk = StringBuilder(overlap)

                // 새 문단 추가
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n")
                }
                currentChunk.append(paragraph)

                // 시작 위치 업데이트 (오버래핑 고려)
                currentStartPos = endPos - overlap.length
            }
            // 최대 크기를 넘으면 강제로 청크 분할
            else {
                val chunkText = currentChunk.toString()
                val endPos = currentStartPos + chunkText.length

                chunks.add(
                    TextChunk(
                        text = chunkText,
                        index = chunkIndex++,
                        startPos = currentStartPos,
                        endPos = endPos
                    )
                )

                // 새 청크 시작
                currentChunk = StringBuilder(paragraph)
                currentStartPos = originalText.indexOf(paragraph, currentStartPos)
            }
        }

        // 마지막 청크 처리
        if (currentChunk.isNotEmpty()) {
            val chunkText = currentChunk.toString()
            chunks.add(
                TextChunk(
                    text = chunkText,
                    index = chunkIndex,
                    startPos = currentStartPos,
                    endPos = currentStartPos + chunkText.length
                )
            )
        }

        return chunks
    }

    /**
     * 오버래핑을 위한 텍스트 추출
     * 청크의 마지막 부분에서 OVERLAP_SIZE만큼 추출
     */
    private fun getOverlapText(chunkText: String): String {
        if (chunkText.length <= OVERLAP_SIZE) {
            return chunkText
        }

        // 마지막 OVERLAP_SIZE 문자를 추출하되, 단어 경계에서 자르기
        val startIndex = chunkText.length - OVERLAP_SIZE
        val overlapCandidate = chunkText.substring(startIndex)

        // 공백으로 시작하지 않으면 첫 공백 이후부터 사용
        val firstSpace = overlapCandidate.indexOf(' ')
        return if (firstSpace > 0 && firstSpace < OVERLAP_SIZE / 2) {
            overlapCandidate.substring(firstSpace + 1)
        } else {
            overlapCandidate
        }
    }
}

/**
 * 텍스트 청크 데이터 클래스
 *
 * @param text 청크의 실제 텍스트 내용
 * @param index 청크 순서 (0부터 시작)
 * @param startPos 원본 텍스트에서의 시작 위치
 * @param endPos 원본 텍스트에서의 끝 위치
 */
data class TextChunk(
    val text: String,
    val index: Int,
    val startPos: Int,
    val endPos: Int
)
