package me.muheun.moaspace.repository

import me.muheun.moaspace.domain.Post
import me.muheun.moaspace.domain.PostEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * PostEmbedding Repository (QueryDSL 마이그레이션)
 * JpaRepository + PostEmbeddingCustomRepository 통합
 */
@Repository
interface PostEmbeddingRepository : JpaRepository<PostEmbedding, Long>, PostEmbeddingCustomRepository {

    /**
     * Post로 PostEmbedding 조회
     */
    fun findByPost(post: Post): Optional<PostEmbedding>

    /**
     * Post ID로 PostEmbedding 존재 여부 확인
     */
    fun existsByPost(post: Post): Boolean

    /**
     * Post로 PostEmbedding 삭제
     */
    fun deleteByPost(post: Post)

    // Native Query findSimilarPosts() 제거 → PostEmbeddingCustomRepository로 이동 (QueryDSL)
}
