-- Alter posts and post_embeddings tables to use bigserial (BIGINT) instead of serial (INTEGER)
-- 이유: JPA Long 타입과 DB INTEGER 타입 불일치 해결
-- 기존 serial (INTEGER) → bigserial (BIGINT) 변환

-- 1. 외래키 제약 조건 삭제
ALTER TABLE post_embeddings DROP CONSTRAINT IF EXISTS post_embeddings_post_id_fkey;

-- 2. posts.id를 BIGINT로 변환
ALTER TABLE posts ALTER COLUMN id TYPE BIGINT;
ALTER TABLE posts ALTER COLUMN author_id TYPE BIGINT;

-- 3. post_embeddings를 BIGINT로 변환
ALTER TABLE post_embeddings ALTER COLUMN id TYPE BIGINT;
ALTER TABLE post_embeddings ALTER COLUMN post_id TYPE BIGINT;

-- 4. 외래키 제약 조건 재생성
ALTER TABLE post_embeddings
ADD CONSTRAINT post_embeddings_post_id_fkey
FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- 5. users.id도 BIGINT로 변환 (일관성)
ALTER TABLE posts DROP CONSTRAINT IF EXISTS posts_author_id_fkey;
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER TABLE posts
ADD CONSTRAINT posts_author_id_fkey
FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE;
