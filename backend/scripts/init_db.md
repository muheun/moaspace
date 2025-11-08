# 데이터베이스 초기 데이터

사용자가 직접 실행하는 초기 데이터 삽입 쿼리입니다.

## vector_configs 초기 데이터

```sql
INSERT INTO vector_configs (entity_type, field_name, weight, enabled, created_at, updated_at)
VALUES
    ('Post', 'title', 2.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Post', 'contentText', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('User', 'name', 1.5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('User', 'email', 1.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (entity_type, field_name)
DO UPDATE SET
    weight = EXCLUDED.weight,
    enabled = EXCLUDED.enabled,
    updated_at = CURRENT_TIMESTAMP;
```
