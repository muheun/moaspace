-- V13: posts 테이블 updated_at 자동 갱신 트리거 제거
-- 이유: 애플리케이션 레벨에서 JPA @PreUpdate로 제어하도록 변경
-- 근본 원인: V5에서 생성된 트리거가 모든 UPDATE 시 updated_at = NOW() 강제 실행
--            V12에서 nullable로 변경했지만 트리거는 그대로 유지되어 문제 발생

-- 트리거 삭제
DROP TRIGGER IF EXISTS update_posts_updated_at ON posts;

-- 트리거 함수는 다른 테이블에서 사용할 수 있으므로 유지
-- (필요시 나중에 별도 마이그레이션으로 제거 가능)
