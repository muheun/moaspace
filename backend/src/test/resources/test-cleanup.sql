-- 테스트 데이터 정리 스크립트
-- Constitution Principle V: 실제 DB 연동 테스트를 위한 테스트 전 정리

-- 외래 키 제약 조건으로 인해 순서 중요:
-- post_embeddings → posts → users 순서로 삭제

-- 1. PostEmbedding 테이블 정리
DELETE FROM post_embeddings;

-- 2. Post 테이블 정리
DELETE FROM posts;

-- 3. User 테이블 정리
DELETE FROM users;

-- 시퀀스 리셋 (AUTO_INCREMENT ID 초기화)
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE posts_id_seq RESTART WITH 1;
ALTER SEQUENCE post_embeddings_id_seq RESTART WITH 1;
