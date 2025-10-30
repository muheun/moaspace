package me.muheun.moaspace.domain

import jakarta.persistence.*
import me.muheun.moaspace.domain.user.User
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(name = "content_markdown", nullable = false, columnDefinition = "TEXT")
    var contentMarkdown: String,

    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    var contentHtml: String,

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    var contentText: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    var author: User,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "TEXT[]")
    var hashtags: Array<String> = emptyArray(),

    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun toString(): String {
        return "Post(id=$id, title='$title', author='$author', createdAt=$createdAt)"
    }
}
