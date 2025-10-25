package me.muheun.moaspace.service

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.springframework.stereotype.Service

/**
 * 마크다운 처리 서비스
 * 마크다운을 순수 텍스트로 변환하여 벡터 임베딩에 사용
 */
@Service
class MarkdownService {

    private val parser: Parser
    private val renderer: HtmlRenderer

    init {
        // Flexmark 옵션 설정
        val options = MutableDataSet()
        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()
    }

    /**
     * 마크다운을 순수 텍스트로 변환
     *
     * 변환 과정:
     * 1. 마크다운 → HTML 파싱
     * 2. HTML 태그 제거
     * 3. 특수 문자 정규화
     *
     * @param markdown 마크다운 형식의 원본 텍스트
     * @return 순수 텍스트 (벡터 임베딩에 사용)
     */
    fun toPlainText(markdown: String): String {
        if (markdown.isBlank()) {
            return ""
        }

        // 1. 마크다운 파싱
        val document = parser.parse(markdown)

        // 2. HTML로 변환 후 태그 제거
        val html = renderer.render(document)
        val plainText = html
            .replace(Regex("<[^>]+>"), " ")  // HTML 태그 제거
            .replace(Regex("&[^;]+;"), " ")  // HTML 엔티티 제거 (&nbsp;, &quot; 등)
            .replace(Regex("\\s+"), " ")     // 연속된 공백을 하나로
            .trim()

        // 3. 추가 정리
        return plainText
            .replace(Regex("[\\r\\n]+"), "\n")  // 연속된 줄바꿈을 하나로
            .trim()
    }

    /**
     * 마크다운을 HTML로 변환
     * (미래 확장용 - 현재는 사용하지 않음)
     */
    fun toHtml(markdown: String): String {
        if (markdown.isBlank()) {
            return ""
        }
        val document = parser.parse(markdown)
        return renderer.render(document)
    }
}
