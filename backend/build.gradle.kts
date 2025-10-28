import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.7.4")
        classpath("org.flywaydb:flyway-database-postgresql:11.15.0")
    }
}

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    kotlin("plugin.jpa") version "1.9.21"
    id("org.flywaydb.flyway") version "11.15.0"
}

group = "me.muheun"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Security + OAuth2 (User Story 1)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // JWT (JSON Web Token)
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Spring Retry (비동기 재시도 메커니즘)
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // PostgreSQL & pgvector
    implementation("org.postgresql:postgresql")  // Flyway에서도 사용하도록 implementation으로 변경
    implementation("com.pgvector:pgvector:0.1.4")

    // Flyway (데이터베이스 마이그레이션 도구)
    // PostgreSQL 18 공식 지원 - 최신 안정 버전 (11.15.0)
    implementation("org.flywaydb:flyway-core:11.15.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")

    // Markdown 처리
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")

    // OpenAI 토크나이저 제거 - 문자 기반 청킹으로 변경 (레퍼런스 프로젝트 참조)
    // implementation("com.knuddels:jtokkit:1.0.0")

    // 형태소 분석기 - Open Korean Text (한국어 텍스트 정규화 및 토큰화)
    implementation("org.openkoreantext:open-korean-text:2.3.1")

    // DJL (Deep Java Library) - ONNX 기반 임베딩 (High-level API)
    implementation("ai.djl:api:0.34.0")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.34.0")
    implementation("ai.djl.huggingface:tokenizers:0.34.0")

    // ONNX Runtime native 라이브러리
    implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")

    // 개발 도구
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    // dotenv
    implementation("me.muheun:spring-dotenv:1.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // 테스트 순차 실행 (병렬 실행으로 인한 DB 동시성 문제 방지)
    maxParallelForks = 1

    // MPNet 모델(1.0GB) 로딩을 위한 메모리 증가
    minHeapSize = "1024m"
    maxHeapSize = "2048m"

    // 테스트 결과 로깅
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// .env 파일 파싱 함수
fun loadEnvFile(envFile: File): Map<String, String> {
    if (!envFile.exists()) {
        throw GradleException(".env file not found at: ${envFile.absolutePath}")
    }

    return envFile.readLines()
        .filter { line ->
            line.isNotBlank() && !line.trim().startsWith("#")
        }
        .mapNotNull { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                    .removePrefix("\"")
                    .removeSuffix("\"")
                key to value
            } else {
                null
            }
        }
        .toMap()
}

// .env 파일에서 DB 설정 읽기
val envFile = file(".env")
val env = loadEnvFile(envFile)

// Flyway 설정 (독립 실행을 위한 DB 연결 정보)
flyway {
    url = env["DB_JDBC_URL"] ?: throw GradleException("DB_JDBC_URL not found in .env")
    user = env["DB_USER"] ?: throw GradleException("DB_USER not found in .env")
    password = env["DB_PASSWORD"] ?: throw GradleException("DB_PASSWORD not found in .env")
    locations = arrayOf("classpath:db/migration")
}
