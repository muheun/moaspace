package me.muheun.moaspace.service

import me.muheun.moaspace.repository.VectorChunkRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

/**
 * VectorProcessingService 유닛 테스트
 *
 * 비동기 이벤트 리스너 대신 processFieldVectorization()을 직접 호출하여
 * 비즈니스 로직만 검증합니다.
 */
@SpringBootTest
@DisplayName("VectorProcessingService 유닛 테스트")
@Sql(
    scripts = ["/test-cleanup.sql"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class VectorProcessingServiceTest {

    @Autowired
    private lateinit var vectorProcessingService: VectorProcessingService

    @Autowired
    private lateinit var vectorChunkRepository: VectorChunkRepository

    @Test
    @DisplayName("필드 벡터화가 정상 동작한다")
    @Transactional
    fun shouldVectorizeFieldSuccessfully() {
        // given
        val namespace = "test"
        val entity = "posts"
        val recordKey = "test-001"
        val fieldValue = "이것은 테스트용 텍스트입니다."

        // when - processFieldVectorization 직접 호출 (비동기 X)
        vectorProcessingService.processFieldVectorization(
            namespace = namespace,
            entity = entity,
            recordKey = recordKey,
            fieldName = "content",
            fieldValue = fieldValue,
            metadata = null
        )

        // then - DB에 청크가 저장되었는지 확인
        val chunks = vectorChunkRepository.findByNamespaceAndEntityAndRecordKeyOrderByChunkIndexAsc(
            namespace, entity, recordKey
        )

        assertThat(chunks).isNotEmpty
        assertThat(chunks).allMatch { it.chunkVector != null }
        assertThat(chunks).allMatch { it.namespace == namespace }

        println("✅ 벡터화 성공: ${chunks.size}개 청크 생성")
    }
}
