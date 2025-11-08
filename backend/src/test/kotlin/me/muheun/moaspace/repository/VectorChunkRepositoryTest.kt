package me.muheun.moaspace.repository

import com.pgvector.PGvector
import me.muheun.moaspace.domain.vector.VectorChunk
import me.muheun.moaspace.domain.vector.VectorConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = ["/test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class VectorChunkRepositoryTest {

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @Autowired
    private lateinit var vectorConfigRepository: VectorConfigRepository

    @BeforeEach
    fun setUp() {
        vectorConfigRepository.saveAll(listOf(
            VectorConfig(
                entityType = "post",
                fieldName = "title",
                weight = 3.0,
                threshold = 0.0,
                enabled = true
            ),
            VectorConfig(
                entityType = "post",
                fieldName = "content",
                weight = 1.0,
                threshold = 0.0,
                enabled = true
            )
        ))
    }

    private fun createTestData() {
        val sampleVectors = listOf(
            floatArrayOf(0.1f, 0.2f, 0.3f) + FloatArray(765) { 0.0f }, // post-1 title
            floatArrayOf(0.2f, 0.3f, 0.4f) + FloatArray(765) { 0.0f }, // post-1 content
            floatArrayOf(0.3f, 0.4f, 0.5f) + FloatArray(765) { 0.0f }, // post-2 title
            floatArrayOf(0.4f, 0.5f, 0.6f) + FloatArray(765) { 0.0f }, // post-2 content
            floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }, // post-3 title (높은 유사도)
            floatArrayOf(0.5f, 0.6f, 0.7f) + FloatArray(765) { 0.0f }  // post-3 content
        )

        val chunks = listOf(
            VectorChunk(namespace = "posts", entity = "post", recordKey = "1", fieldName = "title", chunkText = "Title 1", chunkVector = PGvector(sampleVectors[0]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "1", fieldName = "content", chunkText = "Content 1", chunkVector = PGvector(sampleVectors[1]), chunkIndex = 1, startPosition = 11, endPosition = 30),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "2", fieldName = "title", chunkText = "Title 2", chunkVector = PGvector(sampleVectors[2]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "2", fieldName = "content", chunkText = "Content 2", chunkVector = PGvector(sampleVectors[3]), chunkIndex = 1, startPosition = 11, endPosition = 30),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "3", fieldName = "title", chunkText = "Title 3", chunkVector = PGvector(sampleVectors[4]), chunkIndex = 0, startPosition = 0, endPosition = 10),
            VectorChunk(namespace = "posts", entity = "post", recordKey = "3", fieldName = "content", chunkText = "Content 3", chunkVector = PGvector(sampleVectors[5]), chunkIndex = 1, startPosition = 11, endPosition = 30)
        )

        vectorChunkRepository.saveAll(chunks)
    }

    @Test
    @DisplayName("레코드별 유사도 검색")
    fun testFindSimilarRecords() {
        createTestData()

        // 쿼리 벡터 (post-3 title과 유사)
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        val results = vectorChunkRepository.findSimilarRecords(queryVector, "posts", "post", 3)

        assertThat(results).hasSize(3)
        assertThat(results[0].recordKey).isEqualTo("3") // 가장 유사한 레코드
        assertThat(results[0].score).isGreaterThan(0.9) // 높은 유사도
    }

    @Test
    @DisplayName("레코드별 최상위 청크 검색")
    fun testFindTopChunksByRecord() {
        createTestData()

        // 쿼리 벡터
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        val results = vectorChunkRepository.findTopChunksByRecord(queryVector, "posts", "post", null, 3)

        assertThat(results).hasSize(3)
        assertThat(results.map { it.recordKey }.distinct()).hasSize(3) // 중복 없음
    }

    @Test
    @DisplayName("필드별 가중치 적용 검색")
    fun testFindByWeightedFieldScore() {
        createTestData()

        // 쿼리 벡터
        val queryVector = floatArrayOf(0.9f, 0.9f, 0.9f) + FloatArray(765) { 0.0f }

        val results = vectorChunkRepository.findByWeightedFieldScore(queryVector, "posts", "post", 10)

        assertThat(results).isNotEmpty
        val titleScore = results.find { it.fieldName == "title" }?.weightedScore ?: 0.0
        val contentScore = results.find { it.fieldName == "contentText" }?.weightedScore ?: 0.0
        assertThat(titleScore).isGreaterThan(contentScore) // title 가중치가 더 높음
    }

    @Test
    @DisplayName("nullable fieldName 처리")
    fun testDeleteByFilters() {
        createTestData()

        val deletedAll = vectorChunkRepository.deleteByFilters("posts", "post", "1", null)

        assertThat(deletedAll).isEqualTo(2)

        val deletedTitle = vectorChunkRepository.deleteByFilters("posts", "post", "2", "title")

        assertThat(deletedTitle).isEqualTo(1)

        // 최종 검증: post-1 (0개), post-2 (1개 - content만 남음), post-3 (2개)
        val remaining = vectorChunkRepository.findAll()
        assertThat(remaining).hasSize(3) // 6 - 2 - 1 = 3
    }
}
