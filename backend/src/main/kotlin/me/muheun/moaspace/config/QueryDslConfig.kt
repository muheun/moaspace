package me.muheun.moaspace.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * QueryDSL 설정 클래스
 *
 * JPAQueryFactory를 Bean으로 등록하여 CustomRepository 구현체에서
 * Q클래스 기반 타입 안전한 쿼리를 작성할 수 있도록 합니다.
 *
 * @DataJpaTest 환경에서도 JPAQueryFactory가 활성화되도록 Bean으로 등록
 *
 * JPAQueryFactory 제공 기능:
 * - Q클래스 기반 타입 안전 쿼리 빌딩
 * - 컴파일 타임 쿼리 검증
 * - IDE 자동완성 지원
 */
@Configuration
class QueryDslConfig {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Bean
    fun jpaQueryFactory(): JPAQueryFactory {
        return JPAQueryFactory(entityManager)
    }
}
