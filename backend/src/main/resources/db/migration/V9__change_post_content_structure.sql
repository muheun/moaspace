-- V9__change_post_content_structure.sql
-- Purpose: Post 테이블 컨텐츠 구조 변경 (XSS 방어 및 Markdown 처리)
-- Feature: Post 컨텐츠 필드 재구성
-- Created: 2025-10-30

-- 기존 데이터 백업용 임시 컬럼 생성
ALTER TABLE posts ADD COLUMN IF NOT EXISTS temp_content TEXT;
UPDATE posts SET temp_content = content;

-- 기존 컬럼 삭제
ALTER TABLE posts DROP COLUMN IF EXISTS content;
ALTER TABLE posts DROP COLUMN IF EXISTS plain_content;

-- 새로운 컬럼 추가
ALTER TABLE posts ADD COLUMN content_markdown TEXT NOT NULL DEFAULT '';
ALTER TABLE posts ADD COLUMN content_html TEXT NOT NULL DEFAULT '';
ALTER TABLE posts ADD COLUMN content_text TEXT NOT NULL DEFAULT '';

-- 기존 데이터를 content_html로 복구 (HTML 데이터로 간주)
UPDATE posts SET content_html = temp_content, content_text = regexp_replace(temp_content, '<[^>]*>', '', 'g') WHERE temp_content IS NOT NULL;

-- 임시 컬럼 삭제
ALTER TABLE posts DROP COLUMN IF EXISTS temp_content;

-- 컬럼 주석
COMMENT ON COLUMN posts.content_markdown IS 'Markdown 원본 (편집용)';
COMMENT ON COLUMN posts.content_html IS 'HTML 변환본 (화면 표시용, XSS Sanitize 적용)';
COMMENT ON COLUMN posts.content_text IS '순수 텍스트 (벡터화용, HTML 태그 제거)';
