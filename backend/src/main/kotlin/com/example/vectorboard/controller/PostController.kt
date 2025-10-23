package com.example.vectorboard.controller

import com.example.vectorboard.dto.*
import com.example.vectorboard.service.PostService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    /**
     * 모든 게시글 조회
     */
    @GetMapping
    fun getAllPosts(): ResponseEntity<List<PostResponse>> {
        val posts = postService.getAllPosts()
        return ResponseEntity.ok(posts)
    }

    /**
     * ID로 게시글 조회
     */
    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: Long): ResponseEntity<PostResponse> {
        val post = postService.getPostById(id)
        return ResponseEntity.ok(post)
    }

    /**
     * 게시글 생성
     */
    @PostMapping
    fun createPost(@Valid @RequestBody request: PostCreateRequest): ResponseEntity<PostResponse> {
        val post = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(post)
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody request: PostUpdateRequest
    ): ResponseEntity<PostResponse> {
        val post = postService.updatePost(id, request)
        return ResponseEntity.ok(post)
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        postService.deletePost(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * 벡터 유사도 기반 검색
     */
    @PostMapping("/search/vector")
    fun searchByVector(@Valid @RequestBody request: VectorSearchRequest): ResponseEntity<List<VectorSearchResult>> {
        val results = postService.searchSimilarPosts(request)
        return ResponseEntity.ok(results)
    }

    /**
     * 제목으로 검색
     */
    @GetMapping("/search")
    fun searchByTitle(@RequestParam title: String): ResponseEntity<List<PostResponse>> {
        val results = postService.searchByTitle(title)
        return ResponseEntity.ok(results)
    }
}
