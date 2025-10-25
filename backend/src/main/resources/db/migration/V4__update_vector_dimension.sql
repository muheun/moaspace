-- V4__update_vector_dimension.sql
-- 실제 임베딩 모델(MiniLM-L12-v2)로 전환하기 위해 벡터 차원을 384로 변경

-- 1. 기존 데이터 삭제 (목업 데이터는 재생성 가능)
TRUNCATE TABLE content_chunk CASCADE;

-- 2. 벡터 차원 변경 (128 → 384)
--    기존 차원이 128이 아닌 경우에도 안전하게 384로 변경
ALTER TABLE content_chunk
  ALTER COLUMN chunk_vector TYPE vector(384);

-- 검증: vector(384) 타입 확인
-- psql에서 실행: \d content_chunk
-- 또는 SQL: SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'content_chunk' AND column_name = 'chunk_vector';
