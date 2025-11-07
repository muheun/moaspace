package me.muheun.moaspace.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 벡터 인덱싱 시스템 설정
 *
 * application.yml에서 다음과 같이 설정:
 * ```yaml
 * vector:
 *   namespace: moaspace
 *   multi-tenant: false
 * ```
 *
 * Constitution Principle I 준수:
 * - 운영 설정(VectorConfig)은 DB에 저장
 * - 개발 설정(namespace)은 application.yml에 저장
 */
@Component
@ConfigurationProperties(prefix = "vector")
data class VectorProperties(
    /**
     * 네임스페이스 (멀티테넌시 격리 단위)
     * 기본값: "moaspace"
     *
     * 환경변수 오버라이드 가능:
     * VECTOR_NAMESPACE=blog_system
     */
    var namespace: String = "moaspace",

    /**
     * 멀티테넌트 모드 활성화 여부
     * true: SecurityContext에서 테넌트 ID 동적 추출
     * false: 고정 namespace 사용
     */
    var multiTenant: Boolean = false
)
