-- PostgreSQL 시퀀스 안전 재조정 (V10 수정판)
-- V10의 잘못된 로직을 수정: 시퀀스를 절대 뒤로 되돌리지 않음
--
-- 문제 시나리오 (V10의 위험):
-- - 시퀀스 = 15, MAX(id) = 9 (id=10~14 삭제됨)
-- - V10: setval(9) → 시퀀스가 15에서 9로 후퇴! ❌
-- - 다음 INSERT → id=10 할당 (이미 사용된 값 재사용!)
--
-- 올바른 동작 (V11):
-- - 시퀀스 = 15, MAX(id) = 9
-- - V11: setval(15) → 시퀀스 그대로 유지 ✅
-- - 다음 INSERT → id=16 할당 (id=10~15는 영구 스킵)

-- 시퀀스는 단조증가(monotonically increasing)해야 함
-- GREATEST: 시퀀스 현재값과 MAX(id) 중 큰 값 선택
SELECT setval('users_id_seq', GREATEST(
    (SELECT last_value FROM users_id_seq),
    (SELECT COALESCE(MAX(id), 1) FROM users)
), true);
