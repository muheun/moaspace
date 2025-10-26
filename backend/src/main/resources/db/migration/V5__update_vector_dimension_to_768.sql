-- ===============================================
-- 벡터 차원 변경: 384 → 768
-- ===============================================
-- 목적: multilingual-e5-base 모델로 전환
-- 모델: intfloat/multilingual-e5-base (768차원, 100개 언어 지원)
-- 작성일: 2025-10-26
-- ===============================================

-- 1. 기존 데이터 삭제 (재인덱싱 필요)
-- 주의: 모든 벡터 데이터가 삭제됩니다
TRUNCATE TABLE vector_chunk CASCADE;
TRUNCATE TABLE content_chunk CASCADE;

-- 2. 벡터 차원 변경 (384 → 768)
-- vector_chunk 테이블
ALTER TABLE vector_chunk
  ALTER COLUMN chunk_vector TYPE vector(768);

-- content_chunk 테이블
ALTER TABLE content_chunk
  ALTER COLUMN chunk_vector TYPE vector(768);

-- ===============================================
-- 마이그레이션 완료
-- ===============================================
-- 검증 방법:
-- psql에서 실행:
--   \d vector_chunk
--   \d content_chunk
-- 또는 SQL:
--   SELECT column_name, data_type
--   FROM information_schema.columns
--   WHERE table_name IN ('vector_chunk', 'content_chunk')
--     AND column_name = 'chunk_vector';
-- ===============================================
-- 다음 단계:
-- 1. 애플리케이션 재시작 (새로운 768차원 E5-base 모델 로딩)
-- 2. 재인덱싱 API 호출 또는 배치 작업 실행
-- 3. 벡터 검색 테스트 실행
-- ===============================================
