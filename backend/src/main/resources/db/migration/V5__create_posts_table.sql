-- Posts 테이블 생성 (게시글 저장)
-- Constitution Principle VIII 준수: content (HTML) + plain_content (Plain Text) 분리 저장

-- 기존 테이블 및 의존성 삭제 (개발 환경용 - 프로덕션에서는 주의 필요)
DROP TABLE IF EXISTS post_embeddings CASCADE;
DROP TABLE IF EXISTS posts CASCADE;

CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,               -- HTML 포맷 (Lexical 에디터 출력)
    plain_content TEXT NOT NULL,         -- Plain Text (벡터화 대상)
    author_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hashtags TEXT[] DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE        -- 소프트 삭제 플래그
);

-- 성능 최적화 인덱스
CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX idx_posts_deleted ON posts(deleted) WHERE deleted = FALSE;
CREATE INDEX idx_posts_hashtags ON posts USING GIN(hashtags);

-- updated_at 자동 갱신 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- posts 테이블에 트리거 적용
CREATE TRIGGER update_posts_updated_at
BEFORE UPDATE ON posts
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
