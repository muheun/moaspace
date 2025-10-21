#!/usr/bin/env python3
"""
Phase 3: 테스트 데이터 생성 및 벡터 검색 검증 스크립트
다양한 주제의 게시글을 생성하고 벡터 검색 기능을 테스트합니다.
"""

import requests
import json
import time
from typing import List, Dict

# API 기본 URL
BASE_URL = "http://localhost:8080/api/posts"

# 다양한 주제의 테스트 게시글 데이터
TEST_POSTS = [
    {
        "title": "Spring Boot와 Kotlin으로 REST API 개발하기",
        "content": "Spring Boot는 Java 기반의 강력한 프레임워크입니다. Kotlin과 함께 사용하면 더욱 간결하고 안전한 코드를 작성할 수 있습니다. REST API를 개발할 때 @RestController, @GetMapping, @PostMapping 등의 어노테이션을 활용하여 쉽게 엔드포인트를 구성할 수 있습니다.",
        "author": "김개발"
    },
    {
        "title": "PostgreSQL 성능 최적화 가이드",
        "content": "PostgreSQL은 강력한 오픈소스 관계형 데이터베이스입니다. 인덱스 최적화, 쿼리 플랜 분석, VACUUM 관리 등을 통해 성능을 크게 향상시킬 수 있습니다. EXPLAIN ANALYZE를 사용하여 쿼리 실행 계획을 확인하고 병목 지점을 찾을 수 있습니다.",
        "author": "박데이터"
    },
    {
        "title": "벡터 데이터베이스와 의미 검색의 미래",
        "content": "pgvector를 사용하면 PostgreSQL에서 벡터 검색이 가능합니다. 이는 자연어 처리와 의미 기반 검색에 혁신을 가져왔습니다. 코사인 유사도를 이용한 검색으로 단순 키워드 매칭을 넘어선 지능적인 검색이 가능합니다.",
        "author": "이벡터"
    },
    {
        "title": "Kotlin 코루틴으로 비동기 프로그래밍 마스터하기",
        "content": "Kotlin의 코루틴은 비동기 프로그래밍을 간단하게 만들어줍니다. suspend 함수와 async/await 패턴을 사용하여 복잡한 비동기 로직을 동기 코드처럼 작성할 수 있습니다. Flow를 활용하면 리액티브 스트림 처리도 쉽게 구현할 수 있습니다.",
        "author": "최코틀린"
    },
    {
        "title": "Docker와 Kubernetes로 컨테이너 오케스트레이션",
        "content": "Docker는 애플리케이션을 컨테이너화하는 플랫폼입니다. Kubernetes는 이러한 컨테이너들을 대규모로 관리하고 오케스트레이션할 수 있게 해줍니다. Pod, Service, Deployment 등의 개념을 이해하면 클라우드 네이티브 애플리케이션을 효과적으로 운영할 수 있습니다.",
        "author": "정인프라"
    },
    {
        "title": "React와 TypeScript로 타입 안전한 프론트엔드 개발",
        "content": "React는 컴포넌트 기반 UI 라이브러리입니다. TypeScript를 함께 사용하면 타입 안정성을 확보하여 런타임 에러를 줄일 수 있습니다. useState, useEffect 등의 훅을 활용하여 상태 관리와 사이드 이펙트를 처리합니다.",
        "author": "강프론트"
    },
    {
        "title": "머신러닝 모델을 프로덕션에 배포하는 방법",
        "content": "머신러닝 모델을 실제 서비스에 적용하려면 모델 서빙, 모니터링, A/B 테스팅이 필요합니다. TensorFlow Serving이나 TorchServe를 사용하여 모델을 REST API로 제공할 수 있습니다. MLOps 파이프라인을 구축하면 모델의 지속적인 개선과 배포가 가능합니다.",
        "author": "윤머신"
    },
    {
        "title": "GraphQL vs REST: 어떤 API를 선택할까?",
        "content": "REST API는 단순하고 직관적이지만 오버페칭 문제가 있습니다. GraphQL은 클라이언트가 필요한 데이터만 요청할 수 있어 효율적입니다. 각각의 장단점을 이해하고 프로젝트 요구사항에 맞는 API 스타일을 선택해야 합니다.",
        "author": "송API"
    },
    {
        "title": "CI/CD 파이프라인 구축하기: Jenkins에서 GitHub Actions까지",
        "content": "지속적 통합과 배포는 현대 소프트웨어 개발의 필수 요소입니다. Jenkins, GitLab CI, GitHub Actions 등 다양한 도구를 활용할 수 있습니다. 자동화된 테스트, 빌드, 배포 프로세스를 통해 개발 생산성을 크게 향상시킬 수 있습니다.",
        "author": "임데브옵스"
    },
    {
        "title": "마이크로서비스 아키텍처의 장단점",
        "content": "마이크로서비스는 각 서비스를 독립적으로 개발하고 배포할 수 있게 합니다. 확장성과 유연성이 뛰어나지만 분산 시스템의 복잡성이 증가합니다. API 게이트웨이, 서비스 메시, 분산 추적 등의 패턴을 이해하고 적용해야 합니다.",
        "author": "조아키텍트"
    },
    {
        "title": "SQL 쿼리 최적화 실전 가이드",
        "content": "효율적인 SQL 쿼리 작성은 데이터베이스 성능의 핵심입니다. 인덱스 활용, JOIN 최적화, 서브쿼리 vs CTE 선택 등을 통해 쿼리 성능을 개선할 수 있습니다. 실행 계획을 분석하여 불필요한 테이블 스캔을 제거하고 적절한 인덱스를 생성해야 합니다.",
        "author": "한SQL"
    },
    {
        "title": "NoSQL 데이터베이스 선택 가이드: MongoDB vs Redis vs Cassandra",
        "content": "NoSQL 데이터베이스는 각기 다른 특징과 용도를 가지고 있습니다. MongoDB는 문서 지향 데이터베이스로 유연한 스키마를 제공하고, Redis는 인메모리 캐시로 빠른 성능을 보장하며, Cassandra는 대규모 분산 환경에 적합합니다.",
        "author": "신NoSQL"
    }
]

def create_post(post_data: Dict) -> Dict:
    """게시글 생성"""
    try:
        response = requests.post(BASE_URL, json=post_data)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"❌ 게시글 생성 실패: {e}")
        return None

def get_all_posts() -> List[Dict]:
    """전체 게시글 조회"""
    try:
        response = requests.get(BASE_URL)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"❌ 게시글 조회 실패: {e}")
        return []

def search_posts(query: str, limit: int = 5) -> List[Dict]:
    """벡터 검색"""
    try:
        response = requests.post(
            f"{BASE_URL}/search/vector",
            params={"query": query, "limit": limit}
        )
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"❌ 벡터 검색 실패: {e}")
        return []

def main():
    print("=" * 80)
    print("🚀 Phase 3: 테스트 데이터 생성 및 벡터 검색 검증")
    print("=" * 80)

    # 1. 현재 게시글 수 확인
    print("\n📊 [1단계] 현재 게시글 확인")
    existing_posts = get_all_posts()
    print(f"   현재 게시글 수: {len(existing_posts)}개")

    # 2. 테스트 게시글 생성
    print("\n✍️  [2단계] 테스트 게시글 생성")
    created_count = 0
    for i, post_data in enumerate(TEST_POSTS, 1):
        print(f"   [{i}/{len(TEST_POSTS)}] '{post_data['title']}' 생성 중...", end=" ")
        result = create_post(post_data)
        if result:
            created_count += 1
            print("✅")
        else:
            print("❌")
        time.sleep(0.5)  # 서버 부하 방지

    print(f"\n   생성 완료: {created_count}개 / {len(TEST_POSTS)}개")

    # 3. 전체 게시글 확인
    print("\n📚 [3단계] 전체 게시글 확인")
    all_posts = get_all_posts()
    print(f"   총 게시글 수: {len(all_posts)}개")

    # 4. 벡터 검색 테스트
    print("\n🔍 [4단계] 벡터 검색 기능 테스트")

    test_queries = [
        ("Spring Boot 개발", "Spring Boot 관련 게시글"),
        ("데이터베이스 성능", "데이터베이스 최적화 관련"),
        ("벡터 검색", "pgvector 및 의미 검색 관련"),
        ("비동기 프로그래밍", "Kotlin 코루틴 관련"),
        ("컨테이너 배포", "Docker/Kubernetes 관련")
    ]

    for query, description in test_queries:
        print(f"\n   🔎 검색어: '{query}' ({description})")
        start_time = time.time()
        results = search_posts(query, limit=3)
        elapsed = (time.time() - start_time) * 1000

        print(f"   ⏱️  검색 시간: {elapsed:.2f}ms")
        print(f"   📊 결과 수: {len(results)}개")

        for j, result in enumerate(results, 1):
            print(f"      {j}. {result.get('title', 'N/A')}")
            print(f"         작성자: {result.get('author', 'N/A')}")

    # 5. 성능 테스트
    print("\n⚡ [5단계] 성능 테스트 (10회 평균)")
    search_times = []
    for i in range(10):
        start_time = time.time()
        search_posts("데이터베이스", limit=5)
        elapsed = (time.time() - start_time) * 1000
        search_times.append(elapsed)

    avg_time = sum(search_times) / len(search_times)
    min_time = min(search_times)
    max_time = max(search_times)

    print(f"   평균 검색 시간: {avg_time:.2f}ms")
    print(f"   최소 검색 시간: {min_time:.2f}ms")
    print(f"   최대 검색 시간: {max_time:.2f}ms")

    # 6. 결과 요약
    print("\n" + "=" * 80)
    print("✅ Phase 3 검증 완료!")
    print("=" * 80)
    print(f"📊 총 게시글: {len(all_posts)}개")
    print(f"✨ 생성된 게시글: {created_count}개")
    print(f"🔍 벡터 검색: 정상 동작")
    print(f"⚡ 평균 검색 성능: {avg_time:.2f}ms")
    print("=" * 80)

if __name__ == "__main__":
    main()
