-- pgvector extension 설치 확인 및 생성
CREATE EXTENSION IF NOT EXISTS vector;

-- 설치된 extension 확인
SELECT * FROM pg_extension WHERE extname = 'vector';
