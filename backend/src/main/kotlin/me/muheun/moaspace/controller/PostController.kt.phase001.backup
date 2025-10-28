package me.muheun.moaspace.controller

import me.muheun.moaspace.dto.*
import me.muheun.moaspace.service.PostService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {
    @GetMapping
    fun getAllPosts(): ResponseEntity<List<PostResponse>> {
        val posts = postService.getAllPosts()
        return ResponseEntity.ok(posts)
    }

    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: Long): ResponseEntity<PostResponse> {
        val post = postService.getPostById(id)
        return ResponseEntity.ok(post)
    }

    @PostMapping
    fun createPost(@Valid @RequestBody request: PostCreateRequest): ResponseEntity<PostResponse> {
        val post = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(post)
    }

    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody request: PostUpdateRequest
    ): ResponseEntity<PostResponse> {
        val post = postService.updatePost(id, request)
        return ResponseEntity.ok(post)
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        postService.deletePost(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/search/vector")
    fun searchByVector(@Valid @RequestBody request: PostVectorSearchRequest): ResponseEntity<List<PostVectorSearchResult>> {
        val results = postService.searchSimilarPosts(request)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/search")
    fun searchByTitle(@RequestParam title: String): ResponseEntity<List<PostResponse>> {
        val results = postService.searchByTitle(title)
        return ResponseEntity.ok(results)
    }
}
