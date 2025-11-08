# MoaSpace - 벡터 검색 기반 게시판

PostgreSQL 18 + pgvector를 활용한 의미적 검색 기능이 포함된 현대적 게시판 시스템입니다.

## 🎯 프로젝트 개요

**MoaSpace**는 **범용 벡터 인덱싱 시스템**을 구현한 게시판 프로젝트로, 제목/내용/해시태그/작성자 등 **필드별 개별 벡터화 및 가중치 기반 검색**을 지원합니다. ONNX Runtime 기반 multilingual-e5-base 모델로 한국어 의미 검색이 가능하며, Next.js 15 + React 19 기반의 모던 프론트엔드를 제공합니다.

**핵심 특징:**
- 🔍 **범용 벡터 인덱싱**: `vector_chunk` 테이블로 모든 데이터 타입 벡터화 지원
- ⚖️ **필드별 가중치 설정**: 제목(2.0), 내용(1.0), 해시태그(1.5), 작성자(0.8) 독립 검색
- ⚡ **동시성 최적화**: Semaphore 기반 임베딩 서비스 + Caffeine Cache
- 🎨 **모던 UI**: shadcn/ui + Tiptap 마크다운 에디터

---

## 🏗️ 기술 스택

| 영역 | 기술 | 버전/설명 |
|------|------|-----------|
| **Backend** | Kotlin + Spring Boot | 1.9.21 + 3.2.1 |
| | PostgreSQL + pgvector | 18 + 벡터 확장 |
| | QueryDSL + MyBatis | 5.x + 3.0.3 혼합 전략 |
| | ONNX Runtime | multilingual-e5-base (768차원) |
| **Frontend** | Next.js + React | 15.5.6 (App Router) + 19.1.0 |
| | TypeScript | 5.x (strict mode) |
| | shadcn/ui + Tailwind | 4.x |
| | Tiptap Editor | 3.10.x (마크다운) |
| **Infra** | JDK | 21 |
| | Node.js | 20+ |
| | Gradle | 8.5 |

---

## 🚀 빠른 시작

### 1. 사전 요구사항
- JDK 21, Node.js 20+, PostgreSQL 18 + pgvector
- Google OAuth 2.0 클라이언트 설정

### 2. pgvector extension 설치
```bash
psql -U devuser -d devdb -h localhost -p 15432 -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 3. 환경변수 설정
**backend/.env**:
```env
DB_JDBC_URL=jdbc:postgresql://localhost:15432/devdb
GOOGLE_CLIENT_ID=your-client-id
JWT_SECRET=your-secret-key
FRONTEND_URL=http://localhost:3000
```

**frontend/.env.local**:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4. ONNX 모델 배치
```bash
backend/models/
├── model.onnx
├── tokenizer.json
└── config.json
```
모델 다운로드: [intfloat/multilingual-e5-base](https://huggingface.co/intfloat/multilingual-e5-base)

### 5. 실행
```bash
# Backend
cd backend && ./gradlew bootRun

# Frontend
cd frontend && npm install && npm run dev
```

---

## 💡 구현 완료 기능

### Phase 1-2: 벡터 인프라 구축
- [x] PostgreSQL 18 + pgvector 연동
- [x] ONNX Runtime 기반 임베딩 서비스 (multilingual-e5-base, 768차원)
- [x] 범용 벡터 청크 테이블 (`vector_chunk`) 설계
- [x] 벡터 설정 관리 시스템 (`vector_config`)
- [x] 벡터 유사도 검색 API (코사인 거리)

### Phase 3-5: 게시판 시스템 구현
- [x] Google OAuth 2.0 소셜 로그인
- [x] JWT 토큰 기반 인증 (Spring Security)
- [x] 게시글 CRUD API (작성/조회/수정/삭제)
- [x] Tiptap 마크다운 에디터 통합
- [x] 자동 벡터 임베딩 생성 (게시글 생성/수정 시)
- [x] 해시태그 지원 (배열 필드)
- [x] 소프트 삭제 (복구 가능)
- [x] Next.js 15 + React 19 프론트엔드
- [x] shadcn/ui 기반 모던 UI
- [x] TanStack Query 서버 상태 관리

### Phase 6-7: 필드별 검색 및 최적화
- [x] **필드별 개별 벡터화** (제목/내용/해시태그/작성자)
- [x] **필드별 가중치 설정** (제목 2.0x, 해시태그 1.5x 등)
- [x] **가중치 기반 스코어 계산** (weighted sum)
- [x] 필드 선택 검색 UI (`/posts/search`)
- [x] 동시성 제어 (Semaphore 3개 동시 처리)
- [x] Caffeine Cache (VectorConfig 5분 TTL)
- [x] 성능 테스트 (P95 1초 이내 검증)
- [x] QueryDSL + MyBatis 마이그레이션 완료

### 🚧 향후 계획
- [ ] 댓글 시스템
- [ ] 좋아요/북마크
- [ ] 벡터 인덱스 최적화 (HNSW)
- [ ] 이미지 업로드

---

## 🔬 핵심 아키텍처 결정

### 1. 범용 벡터 인덱싱 시스템

**결정**: 게시글 전용 `post_embeddings` 테이블 대신 범용 `vector_chunk` 테이블 설계

**이유**:
- 다양한 엔티티 타입 (Post, Comment, User 등) 벡터화 지원
- 필드별 개별 벡터화 (제목, 내용, 해시태그, 작성자)
- 동일한 검색 로직으로 모든 엔티티 처리 가능

**구조**:
```sql
CREATE TABLE vector_chunk (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100),    -- 'Post', 'Comment', 'User'
    entity_id BIGINT,             -- 원본 엔티티 ID
    field_name VARCHAR(100),      -- 'title', 'content', 'hashtags'
    chunk_index INT,              -- 청크 순서 (long text 분할 시)
    content TEXT,                 -- 벡터화된 텍스트
    embedding VECTOR(768)         -- 768차원 벡터
);
```

**장점**:
- 확장성: 새로운 엔티티 추가 시 테이블 변경 불필요
- 일관성: 단일 검색 API로 모든 타입 처리
- 유연성: 필드별 독립적 가중치 설정 가능

---

### 2. 필드별 가중치 검색

**결정**: 전체 텍스트 단일 벡터 대신 필드별 개별 벡터 + 가중치 합산 방식

**검색 프로세스**:
1. 쿼리 벡터 생성 (`embedding_query`)
2. 선택된 필드별로 벡터 유사도 계산
3. 필드별 가중치 적용 후 합산
4. 최종 스코어로 정렬

**가중치 설정 예시**:
```json
{
  "entityType": "Post",
  "fieldWeights": {
    "title": 2.0,        // 제목이 가장 중요
    "content": 1.0,      // 내용은 기본 가중치
    "hashtags": 1.5,     // 해시태그 중요도 높음
    "authorName": 0.8    // 작성자는 참고용
  }
}
```

**SQL 쿼리**:
```sql
SELECT entity_id, entity_type,
       SUM((1 - (embedding <=> :queryVector)) * weight) AS weighted_score
FROM vector_chunk vc
JOIN vector_config cfg ON vc.entity_type = cfg.entity_type
WHERE field_name = ANY(:selectedFields)
GROUP BY entity_id, entity_type
HAVING weighted_score >= :threshold
ORDER BY weighted_score DESC
```

**장점**:
- 사용자가 검색 범위 선택 가능 (제목만, 내용만, 전체 등)
- 도메인별 가중치 커스터마이징 (검색 품질 개선)
- 필드별 벡터 재생성 가능 (전체 재생성 불필요)

---

### 3. 동시성 처리 최적화

**문제**: ONNX Runtime 모델 로딩 시 스레드 안전성 이슈 + 메모리 부족

**Phase 1 (초기)**: `@Synchronized` 메서드
- 단점: 한 번에 하나의 요청만 처리 → 성능 병목

**Phase 2 (최종)**: Semaphore(3) + Caffeine Cache
```kotlin
private val semaphore = Semaphore(3)

fun generateEmbedding(text: String): FloatArray {
    semaphore.acquire()
    try {
        return predictor.predict(text).toFloatArray()
    } finally {
        semaphore.release()
    }
}
```

**성능 개선**:
- 동시 3개 요청 처리 가능
- VectorConfig 캐싱으로 DB 조회 90% 감소
- P95 응답 시간 1초 이내 달성

---

### 4. QueryDSL + MyBatis 혼합 전략

**결정**: 단일 ORM 대신 QueryDSL과 MyBatis를 기능별로 선택 사용

**분류 기준**:
- **QueryDSL**: 타입 안전성이 중요한 동적 쿼리
  - 예: 필터링, 정렬, 배열 연산 (`@>`)
- **MyBatis**: QueryDSL이 지원하지 않는 고급 SQL
  - 예: Window Function, CTE, pgvector 코사인 거리 (`<=>`)

**적용 사례**:
```kotlin
// QueryDSL: 동적 필터링
fun findByFilters(entityType: String?, fieldName: String?): List<VectorChunk> {
    return queryFactory.selectFrom(vectorChunk)
        .where(
            entityType?.let { vectorChunk.entityType.eq(it) },
            fieldName?.let { vectorChunk.fieldName.eq(it) }
        )
        .fetch()
}

// MyBatis: pgvector 코사인 거리
@Select("""
    SELECT *, embedding <=> CAST(#{queryVector} AS vector) AS score
    FROM vector_chunk
    ORDER BY score
    LIMIT #{limit}
""")
fun findSimilar(queryVector: String, limit: Int): List<VectorChunk>
```

**장점**:
- 타입 안전성 + SQL 표현력 양립
- 각 도구의 강점 활용
- 유지보수성 향상 (컴파일 타임 검증 + 복잡한 쿼리 가독성)

---

## 📡 벡터 검색 사용 예제

**필드별 가중치 검색**:
```bash
POST http://localhost:8080/api/posts/search/vector
Content-Type: application/json

{
  "query": "Spring Boot 성능 최적화",
  "fields": ["title", "content", "hashtags"],  # 검색할 필드 선택
  "limit": 10,
  "scoreThreshold": 0.6
}
```

**응답 예시**:
```json
[
  {
    "post": {
      "id": 1,
      "title": "Spring Boot 성능 튜닝 가이드",
      "content": "...",
      "hashtags": ["spring", "performance"]
    },
    "fieldScores": {
      "title": 0.92,      // 제목 유사도 (가중치 2.0 적용)
      "content": 0.75,    // 내용 유사도 (가중치 1.0 적용)
      "hashtags": 0.68    // 해시태그 유사도 (가중치 1.5 적용)
    },
    "totalScore": 2.35    // 가중합 점수
  }
]
```

**핵심 특징**:
- 필드별 독립 벡터화 → 선택적 검색 가능
- 가중치 기반 스코어 계산 → 도메인 맞춤 검색 품질
- 코사인 유사도 (`1 - cosine_distance`) 사용

---

## 🧪 테스트

```bash
# Backend (실제 DB 연동 테스트)
cd backend && ./gradlew test

# Frontend (Lint + Build)
cd frontend && npm run lint && npm run build
```

**주요 테스트**:
- 34개 통합 테스트 (실제 PostgreSQL 연동)
- 동시성 테스트 (100개 Post 동시 생성)
- 성능 테스트 (P95 1초 이내 검증)

---

## 🔧 개발 가이드

### 코드 스타일 및 원칙
- Kotlin 1.9.21 표준 컨벤션
- TypeScript strict mode 필수
- **Constitution 원칙** 준수 (`.specify/memory/constitution.md` 참조)

### 데이터 모델
- **User**: Google OAuth 사용자 정보
- **Post**: 게시글 (content HTML + plainContent 분리)
- **VectorChunk**: 범용 벡터 저장소 (entity_type + field_name 기반)
- **VectorConfig**: 필드별 가중치 설정

**자세한 내용**: `CLAUDE.md` 참조

---

## 📚 참고 자료

- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [Next.js 15 Documentation](https://nextjs.org/docs)
- [shadcn/ui](https://ui.shadcn.com/)
- [Tiptap Editor](https://tiptap.dev/)
- [multilingual-e5-base](https://huggingface.co/intfloat/multilingual-e5-base)

---

## 👨‍💻 프로젝트 정보

- **프로젝트명**: MoaSpace
- **시작일**: 2025-10-21
- **최종 업데이트**: 2025-11-06
- **주요 마일스톤**:
  - 2025-10-26: 벡터 인프라 완료
  - 2025-10-29: 게시판 시스템 완료
  - 2025-11-01: QueryDSL 마이그레이션 완료
  - 2025-11-06: 필드별 검색 및 최적화 완료
