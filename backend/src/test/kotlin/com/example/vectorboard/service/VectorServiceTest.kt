package com.example.vectorboard.service

import com.pgvector.PGvector
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("VectorService 테스트")
class VectorServiceTest {

    @Autowired
    private lateinit var vectorService: VectorService

    // VectorService는 stateless이므로 데이터 정리 불필요

    @Test
    @DisplayName("텍스트로부터 벡터를 생성할 수 있다")
    fun `should generate vector from text`() {
        // given
        val text = "이것은 테스트 텍스트입니다. Spring Boot와 pgvector를 사용합니다."

        // when
        val vector = vectorService.generateEmbedding(text)

        // then
        assertThat(vector).isNotNull
        assertThat(vector.toArray()).isNotEmpty
    }

    @Test
    @DisplayName("생성된 벡터의 차원이 올바르다")
    fun `should generate vector with correct dimension`() {
        // given
        val text = "테스트"

        // when
        val vector = vectorService.generateEmbedding(text)

        // then
        assertThat(vector.toArray()).hasSize(VectorService.VECTOR_DIMENSION)
    }

    @Test
    @DisplayName("동일한 텍스트는 동일한 벡터를 생성한다")
    fun `should generate same vector for same text`() {
        // given
        val text = "일관성 테스트"

        // when
        val vector1 = vectorService.generateEmbedding(text)
        val vector2 = vectorService.generateEmbedding(text)

        // then
        assertThat(vector1.toArray()).isEqualTo(vector2.toArray())
    }

    @Test
    @DisplayName("다른 텍스트는 다른 벡터를 생성한다")
    fun `should generate different vectors for different texts`() {
        // given
        val text1 = "Spring Boot"
        val text2 = "Kotlin"

        // when
        val vector1 = vectorService.generateEmbedding(text1)
        val vector2 = vectorService.generateEmbedding(text2)

        // then
        assertThat(vector1.toArray()).isNotEqualTo(vector2.toArray())
    }

    @Test
    @DisplayName("긴 텍스트도 벡터로 변환할 수 있다")
    fun `should handle long text`() {
        // given
        val longText = """
            PostgreSQL은 강력한 오픈소스 관계형 데이터베이스입니다.
            pgvector 확장을 사용하면 벡터 연산이 가능합니다.
            이를 통해 의미 기반 검색을 구현할 수 있습니다.
            코사인 유사도를 이용한 검색으로 관련 문서를 찾을 수 있습니다.
        """.trimIndent()

        // when
        val vector = vectorService.generateEmbedding(longText)

        // then
        assertThat(vector).isNotNull
        assertThat(vector.toArray()).hasSize(VectorService.VECTOR_DIMENSION)
    }

    @Test
    @DisplayName("PGvector를 문자열로 변환할 수 있다")
    fun `should convert PGvector to string`() {
        // given
        val text = "테스트"

        // when
        val vector = vectorService.generateEmbedding(text)
        val vectorString = vector.toString()

        // then
        assertThat(vectorString).isNotNull
        assertThat(vectorString).startsWith("[")
        assertThat(vectorString).endsWith("]")
        assertThat(vectorString).contains(",")
    }
}
