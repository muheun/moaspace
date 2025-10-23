-- 테스트 전 DB 초기화 스크립트
-- 트랜잭션 외부에서 실행되도록 각 테이블을 개별적으로 처리

-- 1. Foreign key constraint를 무시하고 모든 데이터 삭제
SET session_replication_role = replica;

-- 2. 데이터 삭제 (순서대로)
DELETE FROM content_chunks;
DELETE FROM vector_chunk;
DELETE FROM posts;

-- 3. 시퀀스 리셋
ALTER SEQUENCE posts_id_seq RESTART WITH 1;
ALTER SEQUENCE content_chunks_id_seq RESTART WITH 1;
ALTER SEQUENCE vector_chunk_id_seq RESTART WITH 1;

-- 4. Foreign key constraint 다시 활성화
SET session_replication_role = DEFAULT;
