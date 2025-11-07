package me.muheun.moaspace.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 벡터 인덱싱 관련 설정 프로퍼티
 *
 * 하드코딩된 상수를 중앙화하여 환경별 설정 변경을 용이하게 합니다.
 * application.yml의 vector.indexing 섹션에서 값을 주입받습니다.
 */
@Component
@ConfigurationProperties(prefix = "vector.indexing")
data class VectorIndexingProperties(
    /**
     * 청킹 관련 설정
     */
    var chunking: ChunkingConfig = ChunkingConfig(),

    /**
     * 필드 가중치 설정 (엔티티별)
     */
    var fieldWeights: Map<String, EntityFieldWeights> = emptyMap(),

    /**
     * 기본 설정값
     */
    var defaults: DefaultsConfig = DefaultsConfig()
) {
    /**
     * 청킹 설정
     */
    data class ChunkingConfig(
        /** 청크당 최대 토큰 수 */
        var maxTokensPerChunk: Int = 200,

        /** 청크당 목표 토큰 수 */
        var targetTokensPerChunk: Int = 150,

        /** 청크 간 오버랩 토큰 수 */
        var overlapTokens: Int = 50
    )

    /**
     * 엔티티별 필드 가중치
     */
    data class EntityFieldWeights(
        /** 필드명 -> 가중치 매핑 */
        var weights: Map<String, Double> = emptyMap()
    )

    /**
     * 기본 설정값
     */
    data class DefaultsConfig(
        /** 검색 시 기본 limit */
        var searchLimit: Int = 10
    )
}
