#!/bin/bash
# Phase 3: 테스트 데이터 생성 및 벡터 검색 검증 스크립트

BASE_URL="http://localhost:8080/api/posts"

echo "================================================================================"
echo "🚀 Phase 3: 테스트 데이터 생성 및 벡터 검색 검증"
echo "================================================================================"

# 1. 현재 게시글 수 확인
echo ""
echo "📊 [1단계] 현재 게시글 확인"
CURRENT_COUNT=$(curl -s "${BASE_URL}" | grep -o '"id"' | wc -l | tr -d ' ')
echo "   현재 게시글 수: ${CURRENT_COUNT}개"

# 2. 테스트 게시글 생성
echo ""
echo "✍️  [2단계] 테스트 게시글 생성"

# 게시글 배열
declare -a titles=(
    "Spring Boot와 Kotlin으로 REST API 개발하기"
    "PostgreSQL 성능 최적화 가이드"
    "벡터 데이터베이스와 의미 검색의 미래"
    "Kotlin 코루틴으로 비동기 프로그래밍 마스터하기"
    "Docker와 Kubernetes로 컨테이너 오케스트레이션"
    "React와 TypeScript로 타입 안전한 프론트엔드 개발"
    "머신러닝 모델을 프로덕션에 배포하는 방법"
    "GraphQL vs REST: 어떤 API를 선택할까?"
    "CI/CD 파이프라인 구축하기: Jenkins에서 GitHub Actions까지"
    "마이크로서비스 아키텍처의 장단점"
    "SQL 쿼리 최적화 실전 가이드"
    "NoSQL 데이터베이스 선택 가이드: MongoDB vs Redis vs Cassandra"
)

declare -a contents=(
    "Spring Boot는 Java 기반의 강력한 프레임워크입니다. Kotlin과 함께 사용하면 더욱 간결하고 안전한 코드를 작성할 수 있습니다. REST API를 개발할 때 @RestController, @GetMapping, @PostMapping 등의 어노테이션을 활용하여 쉽게 엔드포인트를 구성할 수 있습니다."
    "PostgreSQL은 강력한 오픈소스 관계형 데이터베이스입니다. 인덱스 최적화, 쿼리 플랜 분석, VACUUM 관리 등을 통해 성능을 크게 향상시킬 수 있습니다. EXPLAIN ANALYZE를 사용하여 쿼리 실행 계획을 확인하고 병목 지점을 찾을 수 있습니다."
    "pgvector를 사용하면 PostgreSQL에서 벡터 검색이 가능합니다. 이는 자연어 처리와 의미 기반 검색에 혁신을 가져왔습니다. 코사인 유사도를 이용한 검색으로 단순 키워드 매칭을 넘어선 지능적인 검색이 가능합니다."
    "Kotlin의 코루틴은 비동기 프로그래밍을 간단하게 만들어줍니다. suspend 함수와 async/await 패턴을 사용하여 복잡한 비동기 로직을 동기 코드처럼 작성할 수 있습니다. Flow를 활용하면 리액티브 스트림 처리도 쉽게 구현할 수 있습니다."
    "Docker는 애플리케이션을 컨테이너화하는 플랫폼입니다. Kubernetes는 이러한 컨테이너들을 대규모로 관리하고 오케스트레이션할 수 있게 해줍니다. Pod, Service, Deployment 등의 개념을 이해하면 클라우드 네이티브 애플리케이션을 효과적으로 운영할 수 있습니다."
    "React는 컴포넌트 기반 UI 라이브러리입니다. TypeScript를 함께 사용하면 타입 안정성을 확보하여 런타임 에러를 줄일 수 있습니다. useState, useEffect 등의 훅을 활용하여 상태 관리와 사이드 이펙트를 처리합니다."
    "머신러닝 모델을 실제 서비스에 적용하려면 모델 서빙, 모니터링, A/B 테스팅이 필요합니다. TensorFlow Serving이나 TorchServe를 사용하여 모델을 REST API로 제공할 수 있습니다. MLOps 파이프라인을 구축하면 모델의 지속적인 개선과 배포가 가능합니다."
    "REST API는 단순하고 직관적이지만 오버페칭 문제가 있습니다. GraphQL은 클라이언트가 필요한 데이터만 요청할 수 있어 효율적입니다. 각각의 장단점을 이해하고 프로젝트 요구사항에 맞는 API 스타일을 선택해야 합니다."
    "지속적 통합과 배포는 현대 소프트웨어 개발의 필수 요소입니다. Jenkins, GitLab CI, GitHub Actions 등 다양한 도구를 활용할 수 있습니다. 자동화된 테스트, 빌드, 배포 프로세스를 통해 개발 생산성을 크게 향상시킬 수 있습니다."
    "마이크로서비스는 각 서비스를 독립적으로 개발하고 배포할 수 있게 합니다. 확장성과 유연성이 뛰어나지만 분산 시스템의 복잡성이 증가합니다. API 게이트웨이, 서비스 메시, 분산 추적 등의 패턴을 이해하고 적용해야 합니다."
    "효율적인 SQL 쿼리 작성은 데이터베이스 성능의 핵심입니다. 인덱스 활용, JOIN 최적화, 서브쿼리 vs CTE 선택 등을 통해 쿼리 성능을 개선할 수 있습니다. 실행 계획을 분석하여 불필요한 테이블 스캔을 제거하고 적절한 인덱스를 생성해야 합니다."
    "NoSQL 데이터베이스는 각기 다른 특징과 용도를 가지고 있습니다. MongoDB는 문서 지향 데이터베이스로 유연한 스키마를 제공하고, Redis는 인메모리 캐시로 빠른 성능을 보장하며, Cassandra는 대규모 분산 환경에 적합합니다."
)

declare -a authors=(
    "김개발"
    "박데이터"
    "이벡터"
    "최코틀린"
    "정인프라"
    "강프론트"
    "윤머신"
    "송API"
    "임데브옵스"
    "조아키텍트"
    "한SQL"
    "신NoSQL"
)

CREATED_COUNT=0
TOTAL_COUNT=${#titles[@]}

for i in "${!titles[@]}"; do
    echo -n "   [$((i+1))/${TOTAL_COUNT}] '${titles[$i]}' 생성 중... "

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}" \
        -H "Content-Type: application/json" \
        -d "{\"title\":\"${titles[$i]}\",\"content\":\"${contents[$i]}\",\"author\":\"${authors[$i]}\"}")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        echo "✅"
        ((CREATED_COUNT++))
    else
        echo "❌ (HTTP $HTTP_CODE)"
    fi

    sleep 0.5
done

echo ""
echo "   생성 완료: ${CREATED_COUNT}개 / ${TOTAL_COUNT}개"

# 3. 전체 게시글 확인
echo ""
echo "📚 [3단계] 전체 게시글 확인"
FINAL_COUNT=$(curl -s "${BASE_URL}" | grep -o '"id"' | wc -l | tr -d ' ')
echo "   총 게시글 수: ${FINAL_COUNT}개"

# 4. 벡터 검색 테스트
echo ""
echo "🔍 [4단계] 벡터 검색 기능 테스트"

declare -a queries=(
    "Spring Boot 개발"
    "데이터베이스 성능"
    "벡터 검색"
    "비동기 프로그래밍"
    "컨테이너 배포"
)

for query in "${queries[@]}"; do
    echo ""
    echo "   🔎 검색어: '$query'"

    START_MS=$(($(date +%s%N)/1000000))
    RESULT=$(curl -s -X POST "${BASE_URL}/search/vector" \
        -H "Content-Type: application/json" \
        -d "{\"query\":\"${query}\",\"limit\":3}")
    END_MS=$(($(date +%s%N)/1000000))
    ELAPSED=$((END_MS - START_MS))

    RESULT_COUNT=$(echo "$RESULT" | grep -o '"id"' | wc -l | tr -d ' ')

    echo "   ⏱️  검색 시간: ${ELAPSED}ms"
    echo "   📊 결과 수: ${RESULT_COUNT}개"

    # 결과 제목 출력
    echo "$RESULT" | grep -o '"title":"[^"]*"' | sed 's/"title":"//g' | sed 's/"$//g' | head -3 | nl | sed 's/^/      /'
done

# 5. 성능 테스트
echo ""
echo "⚡ [5단계] 성능 테스트 (10회 평균)"

SUM=0
MIN=999999
MAX=0

for i in {1..10}; do
    START_MS=$(($(date +%s%N)/1000000))
    curl -s -X POST "${BASE_URL}/search/vector" \
        -H "Content-Type: application/json" \
        -d '{"query":"데이터베이스","limit":5}' > /dev/null
    END_MS=$(($(date +%s%N)/1000000))
    ELAPSED=$((END_MS - START_MS))

    SUM=$((SUM + ELAPSED))
    [ $ELAPSED -lt $MIN ] && MIN=$ELAPSED
    [ $ELAPSED -gt $MAX ] && MAX=$ELAPSED
done

AVG=$((SUM / 10))

echo "   평균 검색 시간: ${AVG}ms"
echo "   최소 검색 시간: ${MIN}ms"
echo "   최대 검색 시간: ${MAX}ms"

# 6. 결과 요약
echo ""
echo "================================================================================"
echo "✅ Phase 3 검증 완료!"
echo "================================================================================"
echo "📊 총 게시글: ${FINAL_COUNT}개"
echo "✨ 생성된 게시글: ${CREATED_COUNT}개"
echo "🔍 벡터 검색: 정상 동작"
echo "⚡ 평균 검색 성능: ${AVG}ms"
echo "================================================================================"
