package me.muheun.moaspace.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration

/**
 * Spring Cache 설정
 *
 * VectorConfig 설정 조회 성능 최적화를 위한 캐싱 활성화
 *
 * **Performance Goals**:
 * - 캐시 히트: < 10ms
 * - 캐시 미스: < 100ms
 * - TTL: 5초 (설정 변경 후 자동 반영)
 *
 * **Cache Names**:
 * - `vectorConfig`: VectorConfig 엔티티 설정 캐시
 *
 * **Cache Provider**:
 * - Phase 1: Spring Simple (in-memory)
 * - Phase 2+: Redis (multi-instance 환경)
 */
@Configuration
@EnableCaching
class CacheConfig
