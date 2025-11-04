-- PostgreSQL 시퀀스 재조정
-- 문제: V4 마이그레이션에서 샘플 데이터 INSERT 후 시퀀스가 제대로 업데이트되지 않음
-- 해결: 현재 users 테이블의 MAX(id) 값으로 시퀀스 재설정

-- users 테이블의 최대 ID 값으로 시퀀스 재조정
-- COALESCE: MAX(id)가 NULL이면 1을 반환 (테이블이 비어있는 경우 대비)
-- true: setval의 is_called 파라미터 (다음 nextval()이 증가된 값을 반환하도록 설정)
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
