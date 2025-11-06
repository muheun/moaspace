-- V14: post_embeddings 테이블 삭제
--
-- 목적: PostEmbedding 레거시 시스템 완전 제거
-- 마이그레이션 완료: PostEmbedding → VectorChunk (범용 벡터 인덱싱 시스템)
--
-- 작성일: 2025-11-05

-- post_embeddings 테이블 삭제 (CASCADE로 관련 인덱스도 자동 삭제)
DROP TABLE IF EXISTS post_embeddings CASCADE;

COMMENT ON SCHEMA public IS 'PostEmbedding 시스템 제거 완료, VectorChunk 시스템으로 통합';
