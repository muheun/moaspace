package com.example.vectorai.domain.post

import com.example.vectorai.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 게시글 엔티티 (Markdown 기반)
 *
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 분리 저장
 * Constitution Principle V: 실제 DB 연동 테스트 대상
 */
@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String, // HTML 포맷 (Lexical 에디터 출력)

    @Column(nullable = false, columnDefinition = "TEXT")
    val plainContent: String, // Plain Text (벡터화 대상)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,

    @Column(columnDefinition = "TEXT[]")
    val hashtags: Array<String> = emptyArray(),

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var deleted: Boolean = false
) {
    // Array equality를 위한 equals/hashCode 오버라이드
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Post

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (plainContent != other.plainContent) return false
        if (!hashtags.contentEquals(other.hashtags)) return false
        if (deleted != other.deleted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + plainContent.hashCode()
        result = 31 * result + hashtags.contentHashCode()
        result = 31 * result + deleted.hashCode()
        return result
    }
}
