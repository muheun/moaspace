package me.muheun.moaspace.helper

import me.muheun.moaspace.config.VectorProperties
import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.domain.vector.VectorEntityType
import me.muheun.moaspace.repository.VectorConfigRepository
import org.springframework.stereotype.Component

/**
 * 벡터 테스트 헬퍼 유틸리티
 *
 * - VectorConfig DB 기반 동적 필드 검증
 * - 하드코딩 제거로 메인 로직과 일관성 유지
 * - VectorProperties와 VectorEntityType enum 사용
 */
@Component
class VectorTestHelper(
    private val vectorProperties: VectorProperties,
    private val vectorConfigRepository: VectorConfigRepository
) {

    /**
     * 기본 네임스페이스 조회 (VectorProperties 기반)
     */
    val defaultNamespace: String
        get() = vectorProperties.namespace

    /**
     * 테스트용 Post 객체 생성
     */
    fun createTestPost(
        title: String = "테스트 제목",
        content: String = "테스트 본문 내용입니다.",
        author: User,
        hashtags: Array<String> = emptyArray()
    ): Post {
        return Post(
            title = title,
            contentMarkdown = content,
            contentHtml = "<p>$content</p>",
            contentText = content.replace(Regex("<[^>]*>"), ""), // HTML 태그 제거
            author = author,
            hashtags = hashtags
        )
    }


    /**
     * 테스트용 VectorConfig 생성 (표준 Post 설정)
     *
     * @param namespace 네임스페이스 (기본값: VectorProperties)
     * @return VectorConfig 리스트 (title + contentText)
     */
    fun createStandardPostVectorConfigs(namespace: String? = null): List<VectorConfig> {
        val ns = namespace ?: defaultNamespace
        return listOf(
            VectorConfig(
                namespace = ns,
                entityType = VectorEntityType.POST.typeName,
                fieldName = "title",
                weight = 2.0,
                threshold = 0.0,
                enabled = true
            ),
            VectorConfig(
                namespace = ns,
                entityType = VectorEntityType.POST.typeName,
                fieldName = "contentText",
                weight = 1.0,
                threshold = 0.0,
                enabled = true
            )
        )
    }

}
