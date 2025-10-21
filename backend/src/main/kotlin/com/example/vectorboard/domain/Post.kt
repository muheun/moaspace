package com.example.vectorboard.domain

import com.example.vectorboard.config.PGvectorType
import com.pgvector.PGvector
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, length = 100)
    var author: String,

    @Column(name = "content_vector", columnDefinition = "vector(1536)")
    @Type(PGvectorType::class)
    var contentVector: PGvector? = null,

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
