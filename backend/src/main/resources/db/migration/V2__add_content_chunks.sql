-- 벡터 청킹 시스템을 위한 마이그레이션
-- 작성일: 2025-10-23

-- 1. posts 테이블에 plain_content 컬럼 추가
ALTER TABLE posts
ADD COLUMN IF NOT EXISTS plain_content TEXT;

-- plain_content 컬럼에 대한 설명 추가
COMMENT ON COLUMN posts.plain_content IS '마크다운을 순수 텍스트로 변환한 내용 (벡터 임베딩용)';

-- 1-1. posts 테이블에서 content_vector 컬럼 제거 (청크 기반 검색으로 대체)
ALTER TABLE posts
DROP COLUMN IF EXISTS content_vector;

-- 2. content_chunks 테이블 생성
CREATE TABLE IF NOT EXISTS content_chunks (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_vector vector(1536),
    chunk_index INTEGER NOT NULL,
    start_position INTEGER NOT NULL,
    end_position INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 외래 키 제약조건
    CONSTRAINT fk_content_chunk_post
        FOREIGN KEY (post_id)
        REFERENCES posts(id)
        ON DELETE CASCADE
);

-- 3. 인덱스 생성
-- Post ID 인덱스 (조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_content_chunk_post_id
ON content_chunks(post_id);

-- Chunk 순서 복합 인덱스 (정렬 성능 향상)
CREATE INDEX IF NOT EXISTS idx_content_chunk_index
ON content_chunks(post_id, chunk_index);

-- 벡터 유사도 검색을 위한 IVFFlat 인덱스
-- lists 파라미터는 테이블 행 수에 따라 조정 (일반적으로 sqrt(rows))
-- 초기에는 100으로 설정, 데이터가 많아지면 조정 필요
CREATE INDEX IF NOT EXISTS idx_content_chunk_vector
ON content_chunks
USING ivfflat (chunk_vector vector_cosine_ops)
WITH (lists = 100);

-- 4. 테이블 설명 추가
COMMENT ON TABLE content_chunks IS '게시글 콘텐츠를 청크 단위로 분할하여 저장 (벡터 검색 품질 향상)';
COMMENT ON COLUMN content_chunks.post_id IS '원본 게시글 ID';
COMMENT ON COLUMN content_chunks.chunk_text IS '청크의 실제 텍스트 내용';
COMMENT ON COLUMN content_chunks.chunk_vector IS '청크의 벡터 임베딩 (1536차원)';
COMMENT ON COLUMN content_chunks.chunk_index IS '청크 순서 (0부터 시작)';
COMMENT ON COLUMN content_chunks.start_position IS '원본 텍스트에서의 시작 위치';
COMMENT ON COLUMN content_chunks.end_position IS '원본 텍스트에서의 끝 위치';
