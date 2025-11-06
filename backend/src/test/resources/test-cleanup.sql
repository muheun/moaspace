-- ========================================
-- test-cleanup.sql
-- Purpose: 테스트 데이터 삭제 전용 스크립트
-- ========================================
-- ⚠️ 중요: 이 스크립트는 데이터 삭제만 수행합니다!
-- ⚠️ INSERT, UPDATE 절대 금지!
-- ⚠️ 모든 테이블의 데이터를 삭제합니다! (vector_configs 포함)
-- ========================================
--
-- 사용 위치: @Sql(scripts = ["classpath:test-cleanup.sql"])
-- 실행 시점: 각 테스트 메서드 실행 전 (@BeforeEach 대신 @Sql 사용)
--
-- TRUNCATE 특징:
-- - RESTART IDENTITY: PostgreSQL에서 시퀀스를 1로 리셋
-- - CASCADE: 외래 키 제약 조건이 있어도 삭제 가능
--
-- 삭제 순서: 외래 키 역순 (posts → users, vector_chunks, vector_configs)
-- ========================================

-- 1. posts 테이블 초기화 (author_id FK → users)
TRUNCATE TABLE posts RESTART IDENTITY CASCADE;

-- 2. users 테이블 초기화
TRUNCATE TABLE users RESTART IDENTITY CASCADE;

-- 3. vector_chunks 테이블 초기화
TRUNCATE TABLE vector_chunks RESTART IDENTITY CASCADE;

-- 4. vector_configs 테이블 초기화
TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE;

-- ========================================
-- ⚠️ 참고: 필요한 초기 데이터는 각 테스트의 @BeforeEach에서 생성하세요!
-- ========================================
-- 예시:
-- @BeforeEach
-- fun setUp() {
--     vectorConfigRepository.saveAll(listOf(
--         VectorConfig("Post", "title", 2.0, 0.0, true),
--         VectorConfig("Post", "content", 1.0, 0.0, true)
--     ))
-- }
-- ========================================
