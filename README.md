# MoaSpace - 벡터 검색 기반 게시판 시스템

PostgreSQL 18 + pgvector를 활용한 의미적 검색 기능이 포함된 현대적 게시판 시스템

## 🎯 프로젝트 개요

**MoaSpace**는 **벡터 기반 의미적 검색**과 **현대적인 웹 기술 스택**을 결합한 완전한 게시판 시스템입니다.

### 주요 기능

#### ✅ 인증 및 사용자 관리
- Google OAuth 2.0 기반 소셜 로그인
- JWT 토큰 기반 세션 관리
- 자동 로그인 및 토큰 갱신

#### ✅ 게시글 관리
- 게시글 작성, 조회, 수정, 삭제 (CRUD)
- Lexical 마크다운 에디터 (풍부한 텍스트 편집)
- 해시태그 지원
- 소프트 삭제 (복구 가능)

#### ✅ 벡터 검색
- 자동 벡터 임베딩 생성 (ONNX Runtime + multilingual-e5-base)
- 의미적 유사도 검색 (코사인 거리)
- 실시간 벡터 재생성 (게시글 수정 시)
- 스코어 임계값 필터링

#### ✅ 프론트엔드
- Next.js 15 App Router + React 19
- shadcn/ui 기반 모던 UI
- TanStack Query를 통한 서버 상태 관리
- 반응형 디자인 + 다크모드 지원

---

## 🏗️ 기술 스택

### Backend
- **Language**: Kotlin 1.9.21
- **Framework**: Spring Boot 3.2.1
- **JDK**: Java 21
- **Database**: PostgreSQL 18
- **Vector Extension**: pgvector
- **Build Tool**: Gradle 8.5
- **Migration**: Flyway 11.15.0
- **Authentication**: Spring Security + OAuth2 + JWT
- **Embedding**: ONNX Runtime (multilingual-e5-base, 768차원)

### Frontend
- **Framework**: Next.js 15.5.6 (App Router, Turbopack)
- **UI Library**: React 19.1.0 + React Compiler 1.0
- **Language**: TypeScript 5.x (strict mode)
- **UI Components**: shadcn/ui + Tailwind CSS 4.x
- **Editor**: Lexical 0.38.x (마크다운 편집기)
- **State Management**: TanStack Query 5.90.5
- **HTTP Client**: Axios 1.12.2

---

## 📦 프로젝트 구조

```
MoaSpace/
├── backend/                          # Spring Boot + Kotlin 백엔드
│   ├── src/main/kotlin/me/muheun/moaspace/
│   │   ├── config/                   # 설정 (Security, CORS, etc.)
│   │   ├── domain/                   # 엔티티 (User, Post, PostEmbedding)
│   │   │   ├── Post.kt
│   │   │   ├── PostEmbedding.kt
│   │   │   └── user/User.kt
│   │   ├── repository/               # JPA Repository
│   │   │   ├── PostRepository.kt
│   │   │   ├── PostEmbeddingRepository.kt
│   │   │   └── UserRepository.kt
│   │   ├── service/                  # 비즈니스 로직
│   │   │   ├── PostService.kt
│   │   │   ├── PostVectorService.kt
│   │   │   ├── OnnxEmbeddingService.kt
│   │   │   └── UserService.kt
│   │   ├── controller/               # REST API
│   │   │   ├── AuthController.kt
│   │   │   ├── PostController.kt
│   │   │   └── VectorConfigController.kt
│   │   └── dto/                      # Request/Response DTO
│   ├── src/main/resources/
│   │   ├── db/migration/             # Flyway 마이그레이션 스크립트
│   │   └── application.yml           # Spring Boot 설정
│   ├── models/                       # ONNX 모델 (multilingual-e5-base)
│   ├── .env                          # 환경변수 (DB, OAuth 설정)
│   └── build.gradle.kts
│
├── frontend/                         # Next.js 15 프론트엔드
│   ├── app/                          # Next.js App Router
│   │   ├── (auth)/                   # 인증 라우트 그룹
│   │   │   ├── login/                # 로그인 페이지
│   │   │   └── callback/             # OAuth 콜백
│   │   ├── posts/                    # 게시판 라우트
│   │   │   ├── page.tsx              # 게시글 목록
│   │   │   ├── new/                  # 게시글 작성
│   │   │   └── [id]/                 # 게시글 상세/수정
│   │   ├── layout.tsx                # 루트 레이아웃
│   │   └── page.tsx                  # 홈페이지
│   ├── components/
│   │   ├── ui/                       # shadcn/ui 컴포넌트
│   │   ├── layout/                   # Header, Navigation
│   │   ├── editor/                   # Lexical 마크다운 에디터
│   │   └── posts/                    # 게시판 관련 컴포넌트
│   ├── lib/
│   │   ├── api/                      # Axios 클라이언트 및 API 함수
│   │   └── hooks/                    # TanStack Query Hooks
│   ├── types/api/                    # API 타입 정의
│   └── package.json
│
└── README.md                         # 이 파일
```

---

## 🚀 시작하기

### 1. 사전 요구사항

- **JDK 21** (백엔드)
- **Node.js 20+** (프론트엔드)
- **PostgreSQL 18** + **pgvector extension**
- **Google Cloud Console** OAuth 2.0 클라이언트 설정

### 2. PostgreSQL + pgvector 설정

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

### 3. 환경변수 설정

**backend/.env** 파일 생성:
```env
# Database
DB_JDBC_URL=jdbc:postgresql://localhost:15432/devdb
DB_USER=devuser
DB_PASSWORD=dev123!

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/callback/google

# JWT
JWT_SECRET=your-secure-secret-key-here
JWT_EXPIRATION_MS=3600000

# Frontend URL (CORS)
FRONTEND_URL=http://localhost:3000
```

**frontend/.env.local** 파일 생성:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 4. ONNX 모델 다운로드

multilingual-e5-base 모델을 `backend/models/` 디렉토리에 배치:
```bash
backend/models/
├── model.onnx
├── tokenizer.json
└── config.json
```

모델은 Hugging Face에서 다운로드: `intfloat/multilingual-e5-base`

### 5. 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 실행됩니다.

**Flyway 마이그레이션 수동 실행 (필요 시):**
```bash
./gradlew flywayMigrate
```

### 6. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

프론트엔드가 `http://localhost:3000`에서 실행됩니다.

---

## 📡 API 엔드포인트

### 인증 API

#### Google OAuth 로그인 시작
```bash
GET /api/auth/google/login
# Redirect to Google OAuth consent screen
```

#### OAuth 콜백 처리
```bash
GET /api/auth/callback/google?code={authorization_code}
# Response: JWT 토큰
```

### 게시글 API

#### 게시글 목록 조회
```bash
GET /api/posts
# Response: 게시글 목록 (최신순, deleted=false만)
```

#### 게시글 상세 조회
```bash
GET /api/posts/{id}
# Response: 게시글 상세 정보
```

#### 게시글 작성
```bash
POST /api/posts
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "title": "게시글 제목",
  "content": "<p>HTML 포맷 본문</p>",
  "plainContent": "Plain text 본문",
  "hashtags": ["태그1", "태그2"]
}
```

#### 게시글 수정
```bash
PUT /api/posts/{id}
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "<p>수정된 내용</p>",
  "plainContent": "수정된 내용"
}
```

#### 게시글 삭제 (소프트 삭제)
```bash
DELETE /api/posts/{id}
Authorization: Bearer {jwt_token}
```

### 벡터 검색 API

#### 의미적 검색
```bash
POST /api/posts/search/vector
Content-Type: application/json

{
  "query": "검색어",
  "limit": 10,
  "scoreThreshold": 0.6
}

# Response: 유사도 순으로 정렬된 게시글 목록
```

---

## 🔬 핵심 구현 사항

### 1. 벡터 임베딩 생성 (ONNX Runtime)

게시글 생성/수정 시 자동으로 벡터 임베딩 생성:

```kotlin
// OnnxEmbeddingService.kt
class OnnxEmbeddingService {
    fun generateEmbedding(text: String): FloatArray {
        // multilingual-e5-base 모델 로딩 (768차원)
        val criteria = Criteria.builder()
            .setTypes(NLP.TEXT_EMBEDDING)
            .optModelPath(Paths.get("models"))
            .build()

        // 벡터 생성 (768차원)
        val embedding = predictor.predict(text)
        return embedding.toFloatArray()
    }
}
```

### 2. 벡터 유사도 검색 (pgvector)

코사인 거리를 이용한 의미적 검색:

```kotlin
// PostEmbeddingRepository.kt
@Query(
    value = """
        SELECT pe.*, p.*,
               pe.embedding <=> CAST(:queryVector AS vector) AS similarity_score
        FROM post_embeddings pe
        JOIN posts p ON pe.post_id = p.id
        WHERE p.deleted = FALSE
          AND pe.embedding <=> CAST(:queryVector AS vector) < :threshold
        ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
    """,
    nativeQuery = true
)
fun findSimilarPosts(
    queryVector: String,
    threshold: Double = 0.4,  // 1 - 0.6 = 0.4
    limit: Int = 10
): List<PostEmbedding>
```

**주요 포인트:**
- `<=>` 연산자: pgvector의 코사인 거리 연산자
- 거리가 작을수록 더 유사함 (0.0 = 동일, 2.0 = 완전 반대)
- `threshold`: 유사도 임계값 (0.6 = 60% 이상 유사한 게시글만 반환)
- `ORDER BY`로 유사도 순 정렬

### 3. Next.js App Router + TanStack Query

서버 상태 관리와 클라이언트 상태 분리:

```typescript
// lib/hooks/usePosts.ts
export function usePosts() {
  return useQuery({
    queryKey: ['posts'],
    queryFn: async () => {
      const { data } = await apiClient.get<PostResponse[]>('/api/posts');
      return data;
    }
  });
}

// app/posts/page.tsx
export default function PostsPage() {
  const { data: posts, isLoading } = usePosts();

  if (isLoading) return <Skeleton />;

  return (
    <div>
      {posts?.map(post => (
        <PostCard key={post.id} post={post} />
      ))}
    </div>
  );
}
```

### 4. JWT 인증 흐름

Google OAuth → JWT 토큰 발급 → API 인증:

```kotlin
// JwtTokenService.kt
class JwtTokenService {
    fun generateToken(user: User): String {
        return Jwts.builder()
            .setSubject(user.email)
            .claim("userId", user.id)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(secretKey)
            .compact()
    }
}
```

```typescript
// frontend/lib/api/client.ts
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

---

## 💡 구현 완료 기능

### ✅ 벡터 인프라
- [x] PostgreSQL 18 + pgvector 연동
- [x] ONNX Runtime 기반 임베딩 서비스 (multilingual-e5-base, 768차원)
- [x] 필드별 벡터화 및 가중치 설정 시스템
- [x] 스코어 임계값 필터링
- [x] 벡터 유사도 검색 API

### ✅ 게시판 시스템
- [x] Google OAuth 2.0 소셜 로그인
- [x] JWT 토큰 기반 인증
- [x] 게시글 CRUD API (작성, 조회, 수정, 삭제)
- [x] Lexical 마크다운 에디터
- [x] 자동 벡터 임베딩 생성 (게시글 생성/수정 시)
- [x] 해시태그 지원
- [x] 소프트 삭제
- [x] Next.js 15 + React 19 프론트엔드
- [x] shadcn/ui 기반 모던 UI
- [x] TanStack Query 서버 상태 관리

### 🚧 향후 개선 사항
- [ ] 댓글 시스템
- [ ] 좋아요/북마크 기능
- [ ] 사용자 프로필 페이지
- [ ] 게시글 페이지네이션
- [ ] 벡터 인덱스 최적화 (HNSW)
- [ ] 검색 성능 모니터링
- [ ] 이미지 업로드 기능

---

## 🧪 테스트

### 백엔드 테스트 실행

```bash
cd backend
./gradlew test
```

**주요 특징:**
- 실제 PostgreSQL DB 연동 테스트
- `@Sql` 어노테이션으로 테스트 데이터 초기화
- ONNX 모델 로딩 테스트 포함

### 프론트엔드 테스트 실행

```bash
cd frontend
npm run lint
npm run build
```

---

## 📚 참고 자료

### 백엔드
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [pgvector-java](https://github.com/pgvector/pgvector-java)
- [PostgreSQL 18 Documentation](https://www.postgresql.org/docs/18/)
- [Spring Boot 3.2 Documentation](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [ONNX Runtime](https://onnxruntime.ai/)

### 프론트엔드
- [Next.js 15 Documentation](https://nextjs.org/docs)
- [shadcn/ui](https://ui.shadcn.com/)
- [TanStack Query](https://tanstack.com/query/latest)
- [Lexical Editor](https://lexical.dev/)

### 임베딩 모델
- [multilingual-e5-base (Hugging Face)](https://huggingface.co/intfloat/multilingual-e5-base)

---

## 🔧 개발 가이드

### 코드 스타일

**Kotlin**:
- Kotlin 1.9.21 표준 컨벤션 준수
- 모든 테스트는 실제 DB 연동 필수

**TypeScript**:
- TypeScript strict mode 활용
- Server/Client Component 명확히 분리
- Props Interface 명시 필수

### 데이터 모델

**User** (사용자)
- Google OAuth로 인증된 사용자 정보 저장
- `email`, `name`, `profile_image_url` 필드

**Post** (게시글)
- 게시글 제목, HTML 본문, Plain Text 본문
- `content` (HTML) + `plainContent` (벡터화용) 분리 저장
- 해시태그 지원, 소프트 삭제

**PostEmbedding** (게시글 벡터)
- Post의 `plainContent`를 벡터화한 768차원 임베딩
- `multilingual-e5-base` 모델 사용
- 게시글당 하나의 벡터 (1:1 관계)

---

## 📝 라이센스

이 프로젝트는 학습 및 데모 목적으로 작성되었습니다.

---

## 👨‍💻 개발 정보

- **프로젝트**: MoaSpace
- **시작일**: 2025-10-21
- **최종 업데이트**: 2025-10-29
- **주요 기능**: 벡터 검색, 게시판, OAuth 인증, 마크다운 에디터
- **기술 스택**: Spring Boot + Kotlin, Next.js 15 + React 19, PostgreSQL 18 + pgvector
