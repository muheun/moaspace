package me.muheun.moaspace.service

import me.muheun.moaspace.domain.user.User
import me.muheun.moaspace.domain.vector.VectorConfig
import me.muheun.moaspace.dto.CreatePostRequest
import me.muheun.moaspace.dto.UpdatePostRequest
import me.muheun.moaspace.repository.PostRepository
import me.muheun.moaspace.repository.UserRepository
import me.muheun.moaspace.repository.VectorChunkRepository
import me.muheun.moaspace.repository.VectorConfigRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PostServiceTest @Autowired constructor(
    private val postService: PostService,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val vectorConfigRepository: VectorConfigRepository,
    private val vectorChunkRepository: VectorChunkRepository,
    private val entityManager: EntityManager,
    private val cacheManager: org.springframework.cache.CacheManager
) {

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 캐시 초기화 (VectorConfigService 캐시 제거)
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }

        entityManager.createNativeQuery("TRUNCATE TABLE users RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE posts RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.createNativeQuery("TRUNCATE TABLE vector_chunks RESTART IDENTITY CASCADE").executeUpdate()
        entityManager.flush()
        entityManager.clear()

        testUser = userRepository.save(
            User(
                email = "test@example.com",
                name = "testuser"
            )
        )
    }

    
    @Test
    @DisplayName("Post 생성 시 자동 벡터화")
    fun testCreatePostWithAutoVectorization() {
        // given: VectorConfig 설정
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        val request = CreatePostRequest(
            title = "테스트 게시글 제목",
            contentHtml = "<p>테스트 게시글 본문 내용입니다</p>",
            hashtags = listOf("테스트", "벡터화")
        )

        // when
        val savedPost = postService.createPost(request, testUser.id!!)

        // then: Post 저장 확인
        assertThat(savedPost.id).isNotNull()
        assertThat(savedPost.title).isEqualTo("테스트 게시글 제목")
        assertThat(savedPost.contentText).contains("테스트 게시글 본문 내용입니다")

        // then: 벡터화 확인 (title + content = 2개)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(2)

        val titleChunk = chunks.find { it.fieldName == "title" }
        val contentChunk = chunks.find { it.fieldName == "content" }

        assertThat(titleChunk).isNotNull()
        assertThat(titleChunk?.recordKey).isEqualTo(savedPost.id.toString())
        assertThat(titleChunk?.chunkText).isEqualTo("테스트 게시글 제목")

        assertThat(contentChunk).isNotNull()
        assertThat(contentChunk?.recordKey).isEqualTo(savedPost.id.toString())
        assertThat(contentChunk?.chunkText).contains("테스트 게시글 본문 내용입니다")
    }

    
    @Test
    @DisplayName("VectorConfig 없을 때 벡터화 건너뛰기")
    fun testCreatePostWithoutVectorConfig() {
        // given: VectorConfig 설정 없음

        val request = CreatePostRequest(
            title = "설정 없는 게시글",
            contentHtml = "<p>벡터화 설정이 없습니다</p>",
            hashtags = emptyList()
        )

        // when
        val savedPost = postService.createPost(request, testUser.id!!)

        // then: Post 저장 확인
        assertThat(savedPost.id).isNotNull()

        // then: 벡터화 안됨 (0개)
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).isEmpty()
    }

    
    @Test
    @DisplayName("Post 수정 시 벡터 재생성")
    fun testUpdatePostWithVectorReindexing() {
        // given: VectorConfig 설정 + Post 생성
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        val createRequest = CreatePostRequest(
            title = "원본 제목",
            contentHtml = "<p>원본 본문</p>",
            hashtags = emptyList()
        )

        val originalPost = postService.createPost(createRequest, testUser.id!!)

        // 초기 벡터 확인
        val originalChunks = vectorChunkRepository.findAll()
        assertThat(originalChunks).hasSize(2)

        // when: Post 수정
        val updateRequest = UpdatePostRequest(
            title = "수정된 제목",
            contentHtml = "<p>수정된 본문</p>",
            hashtags = listOf("수정")
        )

        val updatedPost = postService.updatePost(originalPost.id!!, updateRequest, testUser.id!!)

        // then: Post 업데이트 확인
        assertThat(updatedPost.title).isEqualTo("수정된 제목")
        assertThat(updatedPost.contentText).contains("수정된 본문")

        // then: 벡터 재생성 확인
        val updatedChunks = vectorChunkRepository.findAll()
        assertThat(updatedChunks).hasSize(2)

        val titleChunk = updatedChunks.find { it.fieldName == "title" }
        val contentChunk = updatedChunks.find { it.fieldName == "content" }

        assertThat(titleChunk?.chunkText).isEqualTo("수정된 제목")
        assertThat(contentChunk?.chunkText).contains("수정된 본문")
    }

    
    @Test
    @DisplayName("title만 벡터화 설정된 경우")
    fun testCreatePostWithPartialVectorization() {
        // given: title만 enabled
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "title", weight = 2.0, enabled = true)
        )
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = false)
        )

        val request = CreatePostRequest(
            title = "제목만 벡터화",
            contentHtml = "<p>본문은 벡터화 안됨</p>",
            hashtags = emptyList()
        )

        // when
        val savedPost = postService.createPost(request, testUser.id!!)

        // then: title만 벡터화
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks).hasSize(1)
        assertThat(chunks[0].fieldName).isEqualTo("title")
        assertThat(chunks[0].chunkText).isEqualTo("제목만 벡터화")
    }

    
    @Test
    @DisplayName("HTML → PlainText 변환 검증")
    fun testCreatePostHtmlToPlainTextConversion() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        val request = CreatePostRequest(
            title = "HTML 테스트",
            contentHtml = "<p><strong>굵은글씨</strong>와 <em>이탤릭</em> 그리고 <a href='#'>링크</a></p>",
            hashtags = emptyList()
        )

        // when
        val savedPost = postService.createPost(request, testUser.id!!)

        // then: contentText는 순수 텍스트
        assertThat(savedPost.contentText).doesNotContain("<p>", "</p>", "<strong>", "<a>")
        assertThat(savedPost.contentText).contains("굵은글씨", "이탤릭", "링크")

        // then: VectorChunk도 순수 텍스트
        val chunks = vectorChunkRepository.findAll()
        val contentChunk = chunks.find { it.fieldName == "content" }
        assertThat(contentChunk?.chunkText).doesNotContain("<", ">")
        assertThat(contentChunk?.chunkText).contains("굵은글씨")
    }

    
    @Test
    @DisplayName("대용량 content 청킹 검증")
    fun testCreatePostWithLargeContent() {
        // given
        vectorConfigRepository.save(
            VectorConfig(entityType = "Post", fieldName = "content", weight = 1.0, enabled = true)
        )

        // 2000자 텍스트
        val largeContent = "대용량 ".repeat(400) // 약 2000자
        val request = CreatePostRequest(
            title = "대용량 게시글",
            contentHtml = "<p>$largeContent</p>",
            hashtags = emptyList()
        )

        // when
        val savedPost = postService.createPost(request, testUser.id!!)

        // then: 여러 청크 생성
        val chunks = vectorChunkRepository.findAll()
        assertThat(chunks.size).isGreaterThan(1) // 2000자 → 500자 청크 여러 개

        // 모든 청크는 content 필드
        assertThat(chunks.all { it.fieldName == "content" }).isTrue()
        assertThat(chunks.all { it.recordKey == savedPost.id.toString() }).isTrue()
    }
}
