-- V10: posts 테이블의 updated_at 컬럼을 nullable로 변경
-- 실제 수정이 발생했을 때만 updated_at 값을 설정하도록 변경

-- 1. updated_at 컬럼을 nullable로 변경
ALTER TABLE posts ALTER COLUMN updated_at DROP NOT NULL;

-- 2. 기존 데이터 정리: 생성 시간과 동일한 경우 null로 설정
-- (생성만 하고 수정하지 않은 게시글은 updated_at이 null이어야 함)
UPDATE posts
SET updated_at = NULL
WHERE updated_at = created_at;
