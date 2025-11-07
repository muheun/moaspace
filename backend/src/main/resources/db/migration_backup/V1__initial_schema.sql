-- V1: 초기 스키마 (PostgreSQL 18 + pgvector + 768차원)
--
-- 목적: multilingual-e5-base 모델 기반 벡터 검색 시스템의 초기 데이터베이스 스키마 정의
-- 임베딩 모델: intfloat/multilingual-e5-base (768차원)
--
-- 작성일: 2025-10-26

-- ============================================================
-- Step 1: pgvector Extension 활성화
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- Step 2: Posts 테이블 생성
-- ============================================================

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    plain_content TEXT,
    author VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_posts_title ON posts(title);
CREATE INDEX idx_posts_author ON posts(author);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

COMMENT ON TABLE posts IS '게시글 테이블';
COMMENT ON COLUMN posts.plain_content IS '순수 텍스트 본문 (벡터 임베딩용)';

-- ============================================================
-- Step 3: Vector Chunk 테이블 생성
-- ============================================================

CREATE TABLE vector_chunks (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(100) NOT NULL DEFAULT 'vector_ai',
    entity VARCHAR(100) NOT NULL,
    record_key VARCHAR(255) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_vector vector(768),
    chunk_index INTEGER NOT NULL,
    start_position INTEGER NOT NULL,
    end_position INTEGER NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vector_chunk_lookup ON vector_chunks(namespace, entity, record_key);
CREATE INDEX idx_vector_chunk_field ON vector_chunks(namespace, entity, field_name);
CREATE INDEX idx_vector_chunk_order ON vector_chunks(namespace, entity, record_key, chunk_index);

CREATE INDEX idx_vector_chunk_vector
ON vector_chunks
USING ivfflat (chunk_vector vector_cosine_ops)
WITH (lists = 100);

CREATE INDEX idx_vector_chunk_metadata
ON vector_chunks
USING gin (metadata);

COMMENT ON TABLE vector_chunks IS '범용 벡터 청크 저장소 (768차원 - multilingual-e5-base)';
COMMENT ON COLUMN vector_chunks.chunk_vector IS '768차원 벡터 임베딩';

-- ============================================================
-- 완료
-- ============================================================

DO $$
BEGIN
    RAISE NOTICE 'Schema initialized: multilingual-e5-base (768dim)';
END $$;
