-- ========================================
-- V1__initial_schema.sql
-- Purpose: 통합 초기 스키마 (pgvector + 모든 테이블)
-- Created: 2025-11-06
-- Author: Flyway Migration Refactoring
-- ========================================

-- pgvector extension 활성화
CREATE EXTENSION IF NOT EXISTS vector;


-- ========================================
-- 1. users 테이블
-- ========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    profile_image_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);


-- ========================================
-- 2. posts 테이블
-- ========================================
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hashtags TEXT[] DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,

    -- Constitution Principle VIII: content (HTML) + plain content (벡터화용) 분리
    content_markdown TEXT NOT NULL DEFAULT '',  -- Markdown 원본 (편집용)
    content_html TEXT NOT NULL DEFAULT '',      -- HTML 변환본 (화면 표시용, XSS Sanitize 적용)
    content_text TEXT NOT NULL DEFAULT ''       -- 순수 텍스트 (벡터화용, HTML 태그 제거)
);

CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_deleted ON posts(deleted) WHERE deleted = FALSE;
CREATE INDEX idx_posts_hashtags ON posts USING GIN(hashtags);


-- ========================================
-- 3. vector_configs 테이블
-- ========================================
CREATE TABLE vector_configs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    threshold DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_entity_field UNIQUE (entity_type, field_name),
    CONSTRAINT chk_weight_range CHECK (weight >= 0.1 AND weight <= 10.0),
    CONSTRAINT chk_threshold_range CHECK (threshold >= 0.0 AND threshold <= 1.0)
);

CREATE INDEX idx_vector_configs_entity_type ON vector_configs(entity_type);
CREATE INDEX idx_vector_configs_enabled ON vector_configs(enabled);

COMMENT ON TABLE vector_configs IS '엔티티별 벡터화 설정 저장소 (Constitution Principle I)';
COMMENT ON COLUMN vector_configs.entity_type IS '엔티티 타입 (예: Post, Comment)';
COMMENT ON COLUMN vector_configs.field_name IS '벡터화 대상 필드명 (예: title, content)';
COMMENT ON COLUMN vector_configs.weight IS '검색 가중치 (0.1 ~ 10.0, 기본값 1.0)';
COMMENT ON COLUMN vector_configs.threshold IS '유사도 스코어 최소 임계값 (0.0 ~ 1.0, 기본값 0.0)';
COMMENT ON COLUMN vector_configs.enabled IS '활성화 여부 (false일 경우 벡터화 제외)';


-- ========================================
-- 4. vector_chunks 테이블 (pgvector 통합)
-- ========================================
CREATE TABLE vector_chunks (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(100) NOT NULL DEFAULT 'vector_ai',
    entity VARCHAR(100) NOT NULL,
    record_key VARCHAR(255) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_vector vector(768),  -- 768차원 벡터 임베딩 (multilingual-e5-base)
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
CREATE INDEX idx_vector_chunk_metadata ON vector_chunks USING GIN(metadata);

-- pgvector IVFFlat 인덱스 (코사인 유사도)
CREATE INDEX idx_vector_chunk_vector ON vector_chunks USING ivfflat (chunk_vector vector_cosine_ops) WITH (lists = 100);

COMMENT ON TABLE vector_chunks IS '벡터 청크 저장소 (pgvector 통합)';
COMMENT ON COLUMN vector_chunks.chunk_vector IS '768차원 벡터 임베딩';
