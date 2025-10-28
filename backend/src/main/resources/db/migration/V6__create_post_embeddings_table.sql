-- PostEmbeddings 테이블 생성 (게시글 벡터 임베딩 저장)
-- Constitution Principle II 준수: 필드별 벡터화 및 가중치 설정 지원
-- Constitution Principle IV 준수: 임베딩 모델 변경 시 마이그레이션 계획 필요

CREATE TABLE post_embeddings (
    id SERIAL PRIMARY KEY,
    post_id INTEGER UNIQUE NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    embedding vector(768) NOT NULL,     -- 768차원 벡터 (multilingual-e5-base)
    model_name VARCHAR(50) DEFAULT 'multilingual-e5-base',
    created_at TIMESTAMP DEFAULT NOW()
);

-- 벡터 유사도 검색을 위한 IVFFlat 인덱스
-- Constitution Principle III 준수: 스코어 임계값 필터링 지원
CREATE INDEX idx_post_embeddings_vector
ON post_embeddings
USING ivfflat (embedding vector_cosine_ops);

-- post_id 기반 고속 조회를 위한 유니크 인덱스
CREATE UNIQUE INDEX idx_post_embeddings_post ON post_embeddings(post_id);
