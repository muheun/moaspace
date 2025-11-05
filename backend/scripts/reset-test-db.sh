#!/bin/bash

# Test DB 초기화 스크립트
# Flyway 마이그레이션을 V1부터 재실행하기 위해 모든 테이블과 히스토리를 삭제합니다.

set -e

echo "=========================================="
echo "Test DB 초기화 시작"
echo "=========================================="

# .env 파일에서 환경 변수 로드
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"

if [ -f "$ENV_FILE" ]; then
    echo "📄 .env 파일 로드 중: $ENV_FILE"
    export $(grep -v '^#' "$ENV_FILE" | xargs)
else
    echo "⚠️  .env 파일을 찾을 수 없습니다: $ENV_FILE"
fi

# 환경 변수 확인
if [ -z "$DB_JDBC_URL" ] || [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ]; then
    echo "❌ ERROR: DB 환경 변수가 설정되지 않았습니다."
    echo "   필요한 변수: DB_JDBC_URL, DB_USER, DB_PASSWORD"
    echo "   .env 파일을 확인하거나 환경 변수를 직접 설정하세요."
    exit 1
fi

# JDBC URL에서 호스트, 포트, 데이터베이스명 추출
# 예: jdbc:postgresql://localhost:5432/vector_ai_test
DB_INFO=$(echo $DB_JDBC_URL | sed -E 's/jdbc:postgresql:\/\/([^:]+):([0-9]+)\/(.+)/\1 \2 \3/')
DB_HOST=$(echo $DB_INFO | awk '{print $1}')
DB_PORT=$(echo $DB_INFO | awk '{print $2}')
DB_NAME=$(echo $DB_INFO | awk '{print $3}')

echo "DB 호스트: $DB_HOST"
echo "DB 포트: $DB_PORT"
echo "DB 이름: $DB_NAME"
echo "DB 사용자: $DB_USER"
echo ""

# Docker 컨테이너 확인
DOCKER_CONTAINER=$(docker ps --filter "name=postgres" --format "{{.Names}}" | head -n 1)
if [ -z "$DOCKER_CONTAINER" ]; then
    echo "❌ ERROR: PostgreSQL Docker 컨테이너를 찾을 수 없습니다."
    echo "   docker ps로 실행 중인 컨테이너를 확인하세요."
    exit 1
fi
echo "🐳 Docker 컨테이너: $DOCKER_CONTAINER"
echo ""

# PostgreSQL 연결 환경 변수 설정 (Docker exec용)
export PGPASSWORD=$DB_PASSWORD

echo "1️⃣ 기존 스키마 완전 삭제 (CASCADE)..."
docker exec -e PGPASSWORD=$DB_PASSWORD $DOCKER_CONTAINER psql -U $DB_USER -d $DB_NAME -c "DROP SCHEMA IF EXISTS public CASCADE;"
echo "   ✅ 스키마 삭제 완료"
echo ""

echo "2️⃣ public 스키마 재생성..."
docker exec -e PGPASSWORD=$DB_PASSWORD $DOCKER_CONTAINER psql -U $DB_USER -d $DB_NAME -c "CREATE SCHEMA public;"
echo "   ✅ 스키마 생성 완료"
echo ""

echo "3️⃣ pgvector 확장 재설치..."
docker exec -e PGPASSWORD=$DB_PASSWORD $DOCKER_CONTAINER psql -U $DB_USER -d $DB_NAME -c "CREATE EXTENSION IF NOT EXISTS vector;"
echo "   ✅ pgvector 확장 설치 완료"
echo ""

echo "4️⃣ 스키마 권한 설정..."
docker exec -e PGPASSWORD=$DB_PASSWORD $DOCKER_CONTAINER psql -U $DB_USER -d $DB_NAME -c "GRANT ALL ON SCHEMA public TO $DB_USER;"
docker exec -e PGPASSWORD=$DB_PASSWORD $DOCKER_CONTAINER psql -U $DB_USER -d $DB_NAME -c "GRANT ALL ON SCHEMA public TO public;"
echo "   ✅ 권한 설정 완료"
echo ""

echo "=========================================="
echo "✅ Test DB 초기화 완료!"
echo "=========================================="
echo ""
echo "이제 테스트를 실행하면 Flyway가 V1~V15를 순차 실행합니다:"
echo "   ./gradlew test --tests \"*PostServiceTest\""
echo ""
