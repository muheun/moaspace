-- 범용 벡터 인덱싱 시스템으로 마이그레이션
-- content_chunks → vector_chunk로 변환하고 범용 필드 추가
-- 작성일: 2025-10-23

-- ============================================================
-- Step 1: 새로운 vector_chunk 테이블 생성
-- ============================================================

CREATE TABLE IF NOT EXISTS vector_chunk (
    id BIGSERIAL PRIMARY KEY,

    -- 범용 메타데이터 필드 (모든 엔티티 지원)
    namespace VARCHAR(100) NOT NULL DEFAULT 'vector_ai',
    entity VARCHAR(100) NOT NULL,
    record_key VARCHAR(255) NOT NULL,
    field_name VARCHAR(100) NOT NULL,

    -- 청크 내용
    chunk_text TEXT NOT NULL,
    chunk_vector vector(1536),
    chunk_index INTEGER NOT NULL,
    start_position INTEGER NOT NULL,
    end_position INTEGER NOT NULL,

    -- 추가 메타데이터 (JSONB)
    metadata JSONB,

    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Step 2: 기존 content_chunks 데이터 마이그레이션
-- ============================================================

-- content_chunks → vector_chunk로 데이터 복사
-- post_id를 record_key로, entity='posts', field_name='content'로 설정
INSERT INTO vector_chunk (
    namespace,
    entity,
    record_key,
    field_name,
    chunk_text,
    chunk_vector,
    chunk_index,
    start_position,
    end_position,
    metadata,
    created_at
)
SELECT
    'vector_ai' as namespace,
    'posts' as entity,
    post_id::VARCHAR as record_key,
    'content' as field_name,
    chunk_text,
    chunk_vector,
    chunk_index,
    start_position,
    end_position,
    jsonb_build_object('migrated_from', 'content_chunks') as metadata,
    created_at
FROM content_chunks;

-- ============================================================
-- Step 3: 인덱스 생성 (범용 검색 최적화)
-- ============================================================

-- 복합 인덱스 1: 특정 레코드의 모든 청크 조회
-- 용도: 재인덱싱 시 기존 청크 삭제, 레코드별 청크 목록 조회
CREATE INDEX IF NOT EXISTS idx_vector_chunk_lookup
ON vector_chunk(namespace, entity, record_key);

-- 복합 인덱스 2: 특정 필드 검색
-- 용도: 특정 엔티티의 특정 필드에서만 검색
CREATE INDEX IF NOT EXISTS idx_vector_chunk_field
ON vector_chunk(namespace, entity, field_name);

-- 복합 인덱스 3: 청크 순서 조회
-- 용도: 레코드의 청크를 순서대로 조회
CREATE INDEX IF NOT EXISTS idx_vector_chunk_order
ON vector_chunk(namespace, entity, record_key, chunk_index);

-- 벡터 유사도 검색을 위한 IVFFlat 인덱스
-- lists 파라미터는 데이터 규모에 따라 조정 (일반적으로 sqrt(rows))
CREATE INDEX IF NOT EXISTS idx_vector_chunk_vector
ON vector_chunk
USING ivfflat (chunk_vector vector_cosine_ops)
WITH (lists = 100);

-- JSONB 메타데이터 GIN 인덱스 (메타데이터 검색 최적화)
CREATE INDEX IF NOT EXISTS idx_vector_chunk_metadata
ON vector_chunk
USING gin (metadata);

-- ============================================================
-- Step 4: 테이블 및 컬럼 설명 추가
-- ============================================================

COMMENT ON TABLE vector_chunk IS '범용 벡터 청크 저장소 - 모든 엔티티의 텍스트를 청크 단위로 벡터화';
COMMENT ON COLUMN vector_chunk.namespace IS '네임스페이스 (예: vector_ai, my_app)';
COMMENT ON COLUMN vector_chunk.entity IS '엔티티 타입 (예: posts, products, comments)';
COMMENT ON COLUMN vector_chunk.record_key IS '레코드 식별자 (원본 테이블의 ID)';
COMMENT ON COLUMN vector_chunk.field_name IS '필드명 (예: title, content, description)';
COMMENT ON COLUMN vector_chunk.chunk_text IS '청크의 실제 텍스트 내용';
COMMENT ON COLUMN vector_chunk.chunk_vector IS '청크의 벡터 임베딩 (1536차원)';
COMMENT ON COLUMN vector_chunk.chunk_index IS '청크 순서 (0부터 시작)';
COMMENT ON COLUMN vector_chunk.start_position IS '원본 텍스트에서의 시작 위치';
COMMENT ON COLUMN vector_chunk.end_position IS '원본 텍스트에서의 끝 위치';
COMMENT ON COLUMN vector_chunk.metadata IS '추가 메타데이터 (JSONB 형식)';

-- ============================================================
-- Step 5: 기존 content_chunks 테이블 제거
-- ============================================================

-- 기존 인덱스 제거
DROP INDEX IF EXISTS idx_content_chunk_post_id;
DROP INDEX IF EXISTS idx_content_chunk_index;
DROP INDEX IF EXISTS idx_content_chunk_vector;

-- 외래 키 제약조건 제거
ALTER TABLE content_chunks
DROP CONSTRAINT IF EXISTS fk_content_chunk_post;

-- 테이블 제거
DROP TABLE IF EXISTS content_chunks;

-- ============================================================
-- Step 6: 마이그레이션 검증
-- ============================================================

-- 마이그레이션된 데이터 개수 확인 (로그용)
DO $$
DECLARE
    chunk_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO chunk_count FROM vector_chunk;
    RAISE NOTICE 'Migration completed: % chunks migrated to vector_chunk', chunk_count;
END $$;
