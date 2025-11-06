-- 테스트 데이터 정리 스크립트
-- Constitution Principle V: 실제 DB 연동 테스트를 위한 초기화 스크립트
--
-- 사용 위치: @Sql(scripts = ["classpath:test-cleanup.sql"])
-- 실행 시점: 각 테스트 메서드 실행 전 (@BeforeEach 대신 @Sql 사용)
--
-- TRUNCATE vs DELETE:
-- - TRUNCATE: 빠르고, AUTO_INCREMENT 리셋, 트랜잭션 로그 최소화
-- - RESTART IDENTITY: PostgreSQL에서 시퀀스를 1로 리셋
-- - CASCADE: 외래 키 제약 조건이 있어도 삭제 가능

-- VectorChunk 테이블 초기화
TRUNCATE TABLE vector_chunks RESTART IDENTITY CASCADE;

-- VectorConfig 테이블 초기화
TRUNCATE TABLE vector_configs RESTART IDENTITY CASCADE;
