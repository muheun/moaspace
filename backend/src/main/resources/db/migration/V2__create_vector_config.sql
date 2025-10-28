-- V2__create_vector_config.sql
-- Purpose: VectorConfig 엔티티 설정 저장소 테이블 생성 및 기본 데이터 삽입
-- Feature: 001-vector-config (Phase 1)
-- Created: 2025-10-28

-- vector_config 테이블 생성
CREATE TABLE vector_config (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    threshold DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_entity_field UNIQUE (entity_type, field_name),
    CONSTRAINT chk_weight_range CHECK (weight >= 0.1 AND weight <= 10.0),
    CONSTRAINT chk_threshold_range CHECK (threshold >= 0.0 AND threshold <= 1.0)
);

-- 조회 성능 최적화 인덱스
CREATE INDEX idx_vector_config_entity_type ON vector_config(entity_type);
CREATE INDEX idx_vector_config_enabled ON vector_config(enabled);

-- 테이블 및 컬럼 주석
COMMENT ON TABLE vector_config IS '엔티티별 벡터화 설정 저장소 (Phase 1 - 운영 설정 분리)';
COMMENT ON COLUMN vector_config.entity_type IS '엔티티 타입 (예: Post, Comment)';
COMMENT ON COLUMN vector_config.field_name IS '벡터화 대상 필드명 (예: title, content)';
COMMENT ON COLUMN vector_config.weight IS '검색 가중치 (DOUBLE PRECISION, 0.1 ~ 10.0, 기본값 1.0)';
COMMENT ON COLUMN vector_config.threshold IS '유사도 스코어 최소 임계값 (DOUBLE PRECISION, 0.0 ~ 1.0, 기본값 0.0)';
COMMENT ON COLUMN vector_config.enabled IS '활성화 여부 (false일 경우 벡터화 제외)';

-- 기본 데이터 삽입 (Post 엔티티 설정)
INSERT INTO vector_config (entity_type, field_name, weight, threshold, enabled)
VALUES
    ('Post', 'title', 2.0, 0.0, true),
    ('Post', 'content', 1.0, 0.0, true);
