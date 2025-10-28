-- V3__alter_vector_config_columns.sql
-- Purpose: DECIMAL을 DOUBLE PRECISION으로 변경 (Hibernate 호환성)
-- Feature: 001-vector-config (Phase 1 수정)
-- Created: 2025-10-28

-- weight 컬럼 타입 변경: DECIMAL(5,2) → DOUBLE PRECISION
ALTER TABLE vector_config
ALTER COLUMN weight TYPE DOUBLE PRECISION;

-- threshold 컬럼 타입 변경: DECIMAL(4,3) → DOUBLE PRECISION
ALTER TABLE vector_config
ALTER COLUMN threshold TYPE DOUBLE PRECISION;

-- 주석 업데이트
COMMENT ON COLUMN vector_config.weight IS '검색 가중치 (DOUBLE PRECISION, 0.1 ~ 10.0, 기본값 1.0)';
COMMENT ON COLUMN vector_config.threshold IS '유사도 스코어 최소 임계값 (DOUBLE PRECISION, 0.0 ~ 1.0, 기본값 0.0)';
