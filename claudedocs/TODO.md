# PostgreSQL 18 + pgvector 벡터 검색 게시판 프로젝트 TODO

## 🎯 프로젝트 목표
PostgreSQL 18과 pgvector를 활용한 의미적 검색 기능을 포함한 게시판 구현

---

## ✅ Phase 1: 프로젝트 기반 구축 (완료)

- [x] 프로젝트 구조 설계 및 디렉토리 생성
- [x] Spring Boot 백엔드 프로젝트 생성 (Kotlin, JDK21)
- [x] PostgreSQL 연결 설정 (.env 파일 구성)
- [x] 게시판 엔티티 및 리포지토리 구현
- [x] REST API 컨트롤러 구현 (CRUD)

---

## ✅ Phase 2: pgvector 통합 (완료)

### 2.1 pgvector Extension 설정
- [x] PostgreSQL devdb에 pgvector extension 설치 확인
- [x] `CREATE EXTENSION IF NOT EXISTS vector;` 실행
- [x] extension 설치 검증

### 2.2 Hibernate + PGvector 타입 호환성 해결
- [x] PGvector 타입을 Hibernate가 인식하도록 UserType 구현
- [x] Post 엔티티의 contentVector 필드 타입 매핑 수정
- [x] 벡터 저장/조회 테스트

### 2.3 벡터 임베딩 생성
- [x] VectorService에서 목업 벡터 생성 검증
- [x] 게시글 생성 시 자동 벡터 생성 확인
- [x] 벡터 데이터 DB 저장 확인

### 2.4 벡터 검색 쿼리 구현
- [x] PostRepository의 findSimilarPosts 쿼리 테스트
- [x] 코사인 유사도 검색 동작 확인
- [x] 검색 결과 정렬 검증

### 2.5 API 테스트
- [x] POST /api/posts - 게시글 생성 (벡터 포함)
- [x] GET /api/posts - 전체 게시글 조회
- [x] POST /api/posts/search/vector - 벡터 검색
- [x] PUT /api/posts/{id} - 게시글 수정 (벡터 재생성)
- [x] DELETE /api/posts/{id} - 게시글 삭제

---

## ✅ Phase 3: 테스트 데이터 및 검증 (완료)

- [x] 다양한 주제의 게시글 10개 이상 생성 (24개 생성 완료)
- [x] 벡터 검색 기능 실제 동작 테스트 (5가지 검색어 테스트 완료)
- [x] 검색어와 유사한 게시글 반환 확인 (의미 기반 검색 정상 동작)
- [x] 성능 테스트 (검색 속도: 평균 20ms, 매우 우수)

**검증 결과**:
- 총 게시글: 24개
- 벡터 검색: 정상 동작 ✅
- 평균 검색 성능: 20ms
- 테스트 스크립트: `backend/scripts/test_posts.sh`

---

## 🎨 Phase 4: Next.js 프론트엔드 구현 (**현재 진행 중**)

### 4.1 프로젝트 설정
- [ ] Next.js 14 프로젝트 생성 (App Router)
- [ ] shadcn/ui 설치 및 설정
- [ ] Tailwind CSS 설정
- [ ] API 연동 (Tanstack Query)

### 4.2 게시판 UI 구현
- [ ] 게시글 목록 페이지
- [ ] 게시글 작성 폼
- [ ] 게시글 상세 페이지
- [ ] 게시글 수정/삭제 기능

### 4.3 벡터 검색 UI
- [ ] 검색 입력 컴포넌트
- [ ] 유사 게시글 결과 표시
- [ ] 검색 결과 하이라이팅

---

## 🚀 Phase 5: 최종 통합 및 검증

- [ ] 프론트-백 API 완전 연동
- [ ] E2E 테스트
- [ ] README 문서 작성
- [ ] 데모 시나리오 작성

---

## ✅ 해결된 이슈

### **Issue #1: Hibernate + PGvector 타입 변환 오류** (해결 완료)
```
ERROR: column "content_vector" is of type vector but expression is of type bytea
```

**해결 방법**:
- Hibernate UserType 커스텀 구현으로 해결
- PGVector를 String으로 변환 후 PostgreSQL에서 ::vector로 캐스팅
- Native SQL 쿼리를 활용한 벡터 검색 구현

**결과**:
- [x] UserType 구현 완료
- [x] 변환 로직 테스트 완료
- [x] 모든 CRUD 테스트 완료

---

## 🔴 현재 차단 이슈

없음. 모든 백엔드 기능이 정상 동작 중입니다.

---

## 📝 참고 사항

- **DB 연결 정보**: backend/.env 파일 참조
- **pgvector 설치 스크립트**: backend/scripts/init_pgvector.sql
- **벡터 차원**: 1536 (OpenAI text-embedding-3-small 호환)
- **검색 방식**: 코사인 유사도 (<=> 연산자)
