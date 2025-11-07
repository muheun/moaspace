-- V16: vector_configs 테이블에 namespace 컬럼 추가 (데이터 모델 일관성 확보)
-- 목적: vector_chunks와 동일한 격리 수준(isolation level) 제공, 멀티테넌시 지원

-- 1. namespace 컬럼 추가 (기존 데이터 보존: DEFAULT 'moaspace')
ALTER TABLE vector_configs
ADD COLUMN namespace VARCHAR(255) NOT NULL DEFAULT 'moaspace';

-- 2. 기존 UNIQUE 제약조건 삭제
ALTER TABLE vector_configs
DROP CONSTRAINT IF EXISTS uk_vector_configs_entity_field;

-- 3. namespace 포함 UNIQUE 제약조건 생성
ALTER TABLE vector_configs
ADD CONSTRAINT uk_vector_configs_namespace_entity_field
UNIQUE (namespace, entity_type, field_name);

-- 4. 기존 인덱스 삭제
DROP INDEX IF EXISTS idx_configs_entity_enabled;

-- 5. namespace 포함 인덱스 생성 (조회 성능 최적화)
CREATE INDEX idx_configs_namespace_entity_enabled
ON vector_configs(namespace, entity_type, enabled)
WHERE enabled = true;

-- 6. 데이터 검증 (기존 레코드 확인)
DO $$
DECLARE
    record_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO record_count FROM vector_configs;
    RAISE NOTICE 'vector_configs 마이그레이션 완료: % 레코드에 namespace=moaspace 설정됨', record_count;
END $$;
