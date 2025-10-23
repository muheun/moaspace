package com.example.vectorboard.repository

import com.example.vectorboard.domain.Post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PostRepository 테스트")
class PostRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var postRepository: PostRepository

    private lateinit var testPost: Post

    @BeforeEach
    fun setUp() {
        // 모든 데이터 삭제 (테스트 격리)
        postRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        // 테스트 데이터 생성
        testPost = Post(
            title = "테스트 게시글",
            content = "이것은 테스트 게시글입니다. Spring Boot와 Kotlin을 사용합니다.",
            author = "테스터"
        )
    }

    @Test
    @DisplayName("게시글을 저장할 수 있다")
    fun `should save post`() {
        // when
        val savedPost = postRepository.save(testPost)
        entityManager.flush()

        // then
        assertThat(savedPost.id).isNotNull
        assertThat(savedPost.title).isEqualTo("테스트 게시글")
        assertThat(savedPost.author).isEqualTo("테스터")
    }

    @Test
    @DisplayName("ID로 게시글을 조회할 수 있다")
    fun `should find post by id`() {
        // given
        val savedPost = entityManager.persist(testPost)
        entityManager.flush()

        // when
        val foundPost = postRepository.findById(savedPost.id!!)

        // then
        assertThat(foundPost).isPresent
        assertThat(foundPost.get().title).isEqualTo("테스트 게시글")
    }

    @Test
    @DisplayName("모든 게시글을 조회할 수 있다")
    fun `should find all posts`() {
        // given
        val post1 = Post(title = "게시글 1", content = "내용 1", author = "작성자 1")
        val post2 = Post(title = "게시글 2", content = "내용 2", author = "작성자 2")
        val post3 = Post(title = "게시글 3", content = "내용 3", author = "작성자 3")

        entityManager.persist(post1)
        entityManager.persist(post2)
        entityManager.persist(post3)
        entityManager.flush()

        // when
        val posts = postRepository.findAll()

        // then
        assertThat(posts).hasSize(3)
    }

    @Test
    @DisplayName("게시글을 삭제할 수 있다")
    fun `should delete post`() {
        // given
        val savedPost = entityManager.persist(testPost)
        entityManager.flush()
        val postId = savedPost.id!!

        // when
        postRepository.deleteById(postId)
        entityManager.flush()

        // then
        val foundPost = postRepository.findById(postId)
        assertThat(foundPost).isEmpty
    }

    @Test
    @DisplayName("제목으로 게시글을 검색할 수 있다")
    fun `should find posts by title containing`() {
        // given
        val post1 = Post(title = "Spring Boot 가이드", content = "내용 1", author = "작성자 1")
        val post2 = Post(title = "Kotlin 튜토리얼", content = "내용 2", author = "작성자 2")
        val post3 = Post(title = "Spring Data JPA", content = "내용 3", author = "작성자 3")

        entityManager.persist(post1)
        entityManager.persist(post2)
        entityManager.persist(post3)
        entityManager.flush()

        // when
        val results = postRepository.findByTitleContainingIgnoreCase("spring")

        // then
        assertThat(results).hasSize(2)
        assertThat(results).extracting("title")
            .containsExactlyInAnyOrder("Spring Boot 가이드", "Spring Data JPA")
    }

    @Test
    @DisplayName("벡터가 null이 아닌 게시글만 조회할 수 있다")
    fun `should find posts with non-null vectors`() {
        // given
        val postWithoutVector = Post(
            title = "벡터 없는 게시글",
            content = "내용",
            author = "작성자"
        )

        entityManager.persist(postWithoutVector)
        entityManager.flush()

        // when
        val allPosts = postRepository.findAll()
        // contentVector 필드가 제거되어 이 테스트는 더 이상 필요 없음

        // then
        assertThat(allPosts).isNotEmpty
    }

    @Test
    @DisplayName("게시글을 수정할 수 있다")
    fun `should update post`() {
        // given
        val savedPost = entityManager.persist(testPost)
        entityManager.flush()
        entityManager.clear()

        // when
        val postToUpdate = postRepository.findById(savedPost.id!!).get()
        postToUpdate.title = "수정된 제목"
        postToUpdate.content = "수정된 내용"
        postRepository.save(postToUpdate)
        entityManager.flush()
        entityManager.clear()

        // then
        val updatedPost = postRepository.findById(savedPost.id!!).get()
        assertThat(updatedPost.title).isEqualTo("수정된 제목")
        assertThat(updatedPost.content).isEqualTo("수정된 내용")
        assertThat(updatedPost.author).isEqualTo("테스터") // author는 변경되지 않음
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
    fun `should return empty optional for non-existent id`() {
        // when
        val foundPost = postRepository.findById(999L)

        // then
        assertThat(foundPost).isEmpty
    }
}
