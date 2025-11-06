-- V8__rename_vector_config_to_vector_configs.sql
-- Purpose: 테이블 네이밍 컨벤션 일관성 확보 (단수형 → 복수형)
-- Feature: 테이블명 표준화
-- Created: 2025-10-30

-- 기존 vector_config 테이블을 vector_configs로 이름 변경
ALTER TABLE vector_config RENAME TO vector_configs;

-- 인덱스 이름 변경 (일관성 확보)
ALTER INDEX idx_vector_config_entity_type RENAME TO idx_vector_configs_entity_type;
ALTER INDEX idx_vector_config_enabled RENAME TO idx_vector_configs_enabled;

-- 시퀀스 이름 변경
ALTER SEQUENCE vector_config_id_seq RENAME TO vector_configs_id_seq;

-- 테이블 주석 업데이트
COMMENT ON TABLE vector_configs IS '엔티티별 벡터화 설정 저장소 (복수형으로 표준화)';
