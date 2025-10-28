-- 테스트 전 DB 초기화 스크립트
-- TRUNCATE를 사용하여 데이터 삭제 + 시퀀스 자동 리셋

-- 1. Foreign key constraint를 무시하고 모든 데이터 삭제
SET session_replication_role = replica;

-- 2. TRUNCATE로 데이터 삭제 및 시퀀스 리셋 (RESTART IDENTITY)
-- content_chunks 테이블은 Phase 0-2에서 제거됨 (레거시 정리)
TRUNCATE TABLE vector_chunk RESTART IDENTITY CASCADE;
TRUNCATE TABLE posts RESTART IDENTITY CASCADE;
TRUNCATE TABLE vector_config RESTART IDENTITY CASCADE;

-- 3. Foreign key constraint 다시 활성화
SET session_replication_role = DEFAULT;
