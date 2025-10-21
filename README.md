# PostgreSQL 18 + pgvector 벡터 검색 게시판

PostgreSQL 18과 pgvector를 활용한 의미적 검색 기능이 포함된 게시판 시스템

## 🎯 프로젝트 개요

이 프로젝트는 PostgreSQL 18의 pgvector extension을 활용하여 **벡터 기반 의미적 검색**을 구현한 게시판 시스템입니다.

### 주요 기능

- ✅ **게시글 CRUD**: 게시글 생성, 조회, 수정, 삭제
- ✅ **자동 벡터 생성**: 게시글 내용을 벡터로 자동 변환하여 저장
- ✅ **벡터 유사도 검색**: 검색어와 의미가 유사한 게시글 검색 (코사인 유사도)
- ✅ **실시간 벡터 재생성**: 게시글 수정 시 벡터 자동 업데이트

---

## 🏗️ 기술 스택

### Backend
- **Language**: Kotlin 1.9.21
- **Framework**: Spring Boot 3.2.1
- **JDK**: Java 21
- **Database**: PostgreSQL 18
- **Vector Extension**: pgvector
- **Build Tool**: Gradle 8.5

### Frontend (예정)
- Next.js 14
- shadcn/ui
- Tailwind CSS

---

## 📦 프로젝트 구조

```
vector_ai_server/
├── backend/                          # Spring Boot + Kotlin
│   ├── src/main/kotlin/com/example/vectorboard/
│   │   ├── config/
│   │   │   ├── DotEnvLoader.kt      # 환경변수 로더
│   │   │   ├── PGvectorType.kt      # Hibernate UserType for pgvector
│   │   │   └── GlobalExceptionHandler.kt
│   │   ├── domain/
│   │   │   └── Post.kt              # 게시글 엔티티 (vector 필드 포함)
│   │   ├── repository/
│   │   │   └── PostRepository.kt    # pgvector 쿼리 포함
│   │   ├── service/
│   │   │   ├── PostService.kt       # 비즈니스 로직
│   │   │   └── VectorService.kt     # 벡터 임베딩 생성
│   │   └── controller/
│   │       └── PostController.kt    # REST API
│   ├── .env                          # DB 연결 정보
│   ├── build.gradle.kts
│   └── scripts/
│       └── init_pgvector.sql        # pgvector 설치 스크립트
├── frontend/                         # Next.js (TODO)
└── claudedocs/
    └── TODO.md                       # 프로젝트 TODO 리스트
```

---

## 🚀 시작하기

### 1. PostgreSQL + pgvector 설정

**pgvector extension 설치 확인:**
```sql
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

**extension이 없으면 설치:**
```bash
psql -U devuser -d devdb -h localhost -p 15432 -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

또는 제공된 스크립트 사용:
```bash
psql -U devuser -d devdb -h localhost -p 15432 -f backend/scripts/init_pgvector.sql
```

### 2. 환경변수 설정

`backend/.env` 파일이 이미 생성되어 있습니다:
```env
DB_JDBC_URL=jdbc:postgresql://localhost:15432/devdb
DB_USER=devuser
DB_PASSWORD=dev123!
```

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

---

## 📡 API 사용법

### 1. 게시글 생성
```bash
curl -X POST 'http://localhost:8080/api/posts' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "PostgreSQL과 pgvector 시작하기",
    "content": "PostgreSQL 18과 pgvector를 사용하여 벡터 검색을 구현합니다.",
    "author": "김개발"
  }'
```

**응답:**
```json
{
  "id": 1,
  "title": "PostgreSQL과 pgvector 시작하기",
  "content": "PostgreSQL 18과 pgvector를 사용하여 벡터 검색을 구현합니다.",
  "author": "김개발",
  "hasVector": true,
  "createdAt": "2025-10-21T17:21:30.706191",
  "updatedAt": "2025-10-21T17:21:30.706208"
}
```

### 2. 전체 게시글 조회
```bash
curl 'http://localhost:8080/api/posts'
```

### 3. 벡터 검색 (의미적 검색)
```bash
curl -X POST 'http://localhost:8080/api/posts/search/vector' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "데이터베이스 벡터 검색",
    "limit": 5
  }'
```

### 4. 게시글 수정
```bash
curl -X PUT 'http://localhost:8080/api/posts/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "content": "수정된 내용입니다. 벡터가 자동으로 재생성됩니다."
  }'
```

### 5. 게시글 삭제
```bash
curl -X DELETE 'http://localhost:8080/api/posts/1'
```

---

## 🔬 핵심 구현 사항

### 1. Hibernate + pgvector 타입 호환성 해결

pgvector의 `PGvector` 타입을 Hibernate가 인식할 수 있도록 **UserType 구현**:

```kotlin
// backend/src/main/kotlin/com/example/vectorboard/config/PGvectorType.kt
class PGvectorType : UserType<PGvector> {
    override fun nullSafeSet(st: PreparedStatement, value: PGvector?, index: Int, session: SharedSessionContractImplementor?) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            val pgObject = org.postgresql.util.PGobject()
            pgObject.type = "vector"
            pgObject.value = value.toString()
            st.setObject(index, pgObject)
        }
    }
    // ... 기타 메서드
}
```

### 2. Post 엔티티의 vector 필드

```kotlin
@Entity
@Table(name = "posts")
class Post(
    @Column(name = "content_vector", columnDefinition = "vector(1536)")
    @Type(PGvectorType::class)
    var contentVector: PGvector? = null,
    // ... 기타 필드
)
```

### 3. 벡터 유사도 검색 쿼리

```kotlin
@Query(
    value = """
        SELECT p.*,
               p.content_vector <=> CAST(:queryVector AS vector) AS similarity_score
        FROM posts p
        WHERE p.content_vector IS NOT NULL
        ORDER BY p.content_vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
    """,
    nativeQuery = true
)
fun findSimilarPosts(queryVector: String, limit: Int = 10): List<Post>
```

**주요 포인트:**
- `<=>` 연산자: pgvector의 코사인 거리 연산자
- 거리가 작을수록 더 유사함
- `ORDER BY`로 유사도 순 정렬

---

## 💡 현재 구현 상태

### ✅ 완료된 기능
- [x] Spring Boot + Kotlin + JDK21 백엔드 구축
- [x] PostgreSQL 18 + pgvector 연동
- [x] Hibernate UserType으로 pgvector 타입 호환성 해결
- [x] 게시글 CRUD API
- [x] 자동 벡터 생성 (게시글 생성/수정 시)
- [x] 벡터 유사도 기반 검색 API
- [x] 예외 처리 및 에러 응답

### 🚧 개선 사항
- [ ] 실제 임베딩 모델 연동 (현재는 목업 벡터 사용)
  - OpenAI API (text-embedding-3-small)
  - 로컬 모델 (sentence-transformers)
- [ ] Next.js 프론트엔드 구현
- [ ] 벡터 인덱스 최적화 (HNSW)
- [ ] 검색 성능 모니터링

---

## 🔧 임베딩 모델 교체 방법

현재는 목업 벡터를 사용하지만, `VectorService.kt`의 `generateEmbedding` 메서드를 수정하여 실제 임베딩 모델로 교체 가능:

```kotlin
// 예: OpenAI API 사용
fun generateEmbedding(text: String): PGvector {
    val response = openAiClient.createEmbedding(text)
    return PGvector(response.embedding.toFloatArray())
}
```

---

## 📚 참고 자료

- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [pgvector-java](https://github.com/pgvector/pgvector-java)
- [PostgreSQL 18 Documentation](https://www.postgresql.org/docs/18/)
- [Spring Boot 3.2 Documentation](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)

---

## 📝 라이센스

이 프로젝트는 학습 및 테스트 목적으로 작성되었습니다.

---

## 👨‍💻 작성자

- 백엔드: Claude Code (Anthropic)
- 프로젝트: PostgreSQL 18 + pgvector 벡터 검색 데모
