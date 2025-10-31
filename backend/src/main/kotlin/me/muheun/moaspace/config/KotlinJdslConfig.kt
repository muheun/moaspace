package me.muheun.moaspace.config

import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Kotlin JDSL 설정 클래스
 *
 * KotlinJdslJpqlExecutor가 JPQL을 렌더링할 때 사용하는 컨텍스트를 제공합니다.
 * CustomRepository 구현체에서 타입 안전한 쿼리 빌딩을 위해 필수적입니다.
 *
 * @DataJpaTest 환경에서도 KotlinJdslJpqlExecutor가 활성화되도록 KotlinJdslAutoConfiguration import
 *
 * KotlinJdslAutoConfiguration이 자동으로 제공하는 bean:
 * - KotlinJdslJpqlExecutor
 * - JpqlRenderContext
 * - JpqlRenderer
 */
@Configuration
@Import(KotlinJdslAutoConfiguration::class)
class KotlinJdslConfig
