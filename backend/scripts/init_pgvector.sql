-- pgvector extension 설치
-- PostgreSQL에 연결 후 이 스크립트를 실행하세요
-- psql -U devuser -d devdb -h localhost -p 15432 -f scripts/init_pgvector.sql

-- pgvector extension 생성
CREATE EXTENSION IF NOT EXISTS vector;

-- extension 설치 확인
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';

-- 간단한 벡터 테스트
SELECT '[1,2,3]'::vector;

-- 완료 메시지
SELECT 'pgvector extension이 성공적으로 설치되었습니다!' AS status;
