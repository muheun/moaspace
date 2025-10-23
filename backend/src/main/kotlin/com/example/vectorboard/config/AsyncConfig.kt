package com.example.vectorboard.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 비동기 처리 설정
 * 벡터 생성을 백그라운드에서 처리하기 위한 설정
 */
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 비동기 작업을 위한 ThreadPool 설정
     */
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5  // 기본 스레드 수
        executor.maxPoolSize = 10  // 최대 스레드 수
        executor.queueCapacity = 25  // 대기 큐 크기
        executor.setThreadNamePrefix("VectorAsync-")
        executor.initialize()
        return executor
    }

    /**
     * 비동기 작업 중 발생한 예외 처리
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { throwable, method, params ->
            logger.error(
                "비동기 작업 실패: method=${method.name}, params=${params.joinToString()}, error=${throwable.message}",
                throwable
            )
        }
    }
}
