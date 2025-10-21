# 테스트 정책 (Testing Policy)

**이 프로젝트는 Mock을 사용하지 않고, 실제 PostgreSQL DB로 모든 테스트를 수행합니다.**

## 🔴 핵심 원칙

### ❌ Mock 사용 절대 금지
- MockMvc, Mockito, MockBean 등 모든 Mock 라이브러리 사용 금지
- 실제 객체와 실제 DB를 사용한 진짜 테스트만 작성
- "진짜를 테스트하지 않으면 의미가 없다"

### ✅ Real DB 사용 필수
- 모든 테스트는 실제 PostgreSQL DB를 사용
- 별도의 test profile 불필요 (기존 DB 설정 그대로 사용)
- `@AutoConfigureTestDatabase(replace = NONE)` 사용

## 📋 테스트 작성 가이드

### Repository 테스트
```kotlin
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {
    @Autowired
    private lateinit var postRepository: PostRepository

    @BeforeEach
    fun setUp() {
        // 모든 데이터 삭제 (테스트 격리)
        postRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `실제 DB에 데이터를 저장하고 조회한다`() {
        // 실제 PostgreSQL에 저장
        val post = postRepository.save(...)
        // 실제 PostgreSQL에서 조회
        val found = postRepository.findById(post.id)
        assertThat(found).isPresent
    }
}
```

### Service 테스트
```kotlin
@SpringBootTest
class VectorServiceTest {
    @Autowired
    private lateinit var vectorService: VectorService

    @Test
    fun `실제 벡터 생성 로직을 테스트한다`() {
        // Mock 없이 실제 서비스 로직 실행
        val vector = vectorService.generateEmbedding("테스트")
        assertThat(vector).isNotEmpty()
    }
}
```

### 통합 테스트 (Integration Test)
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VectorSearchIntegrationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var postRepository: PostRepository

    @BeforeEach
    fun setUp() {
        // 모든 데이터 삭제 (테스트 격리)
        postRepository.deleteAll()
        assertThat(postRepository.count()).isEqualTo(0)
    }

    @Test
    fun `실제 HTTP 요청으로 E2E 테스트한다`() {
        // 실제 HTTP POST 요청
        val response = restTemplate.postForEntity(...)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
```

## ⚠️ 필수 사항

### ✅ 테스트 격리 (Test Isolation)
**모든 테스트는 독립적으로 실행되어야 합니다.**

```kotlin
@BeforeEach
fun setUp() {
    // 반드시 DB를 비워야 함!
    repository.deleteAll()
    entityManager.flush()
    entityManager.clear()
}
```

**테스트 간 데이터 공유는 절대 금지입니다.**

## 🚫 금지 사항

### ❌ 사용하지 말 것
- `@MockBean`
- `@Mock`
- `Mockito.mock()`
- `MockMvc` (WebMvcTest)
- `@WebMvcTest`
- `@SpringBootTest` + MockMvc
- 모든 Stub, Spy, Fake 객체

### ✅ 사용할 것
- `TestRestTemplate` - 실제 HTTP 호출
- `@DataJpaTest` - 실제 DB 테스트
- `@SpringBootTest` - 전체 컨텍스트 로드
- `@Transactional` - 테스트 후 롤백

## 📊 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests VectorSearchIntegrationTest

# 테스트 with 상세 로그
./gradlew test --info
```

## 🎯 테스트 커버리지 목표

- Repository: 100% (모든 쿼리 메서드 테스트)
- Service: 90% (핵심 비즈니스 로직)
- Integration: 80% (주요 사용자 시나리오)

## 💡 왜 Mock을 사용하지 않는가?

1. **진짜를 테스트한다**: Mock은 가짜 동작을 테스트하는 것
2. **DB 통합 오류 발견**: 실제 DB에서만 발견되는 버그 포착
3. **벡터 검색 검증**: pgvector 쿼리는 실제 DB에서만 정확히 테스트 가능
4. **프로덕션 신뢰성**: 실제 환경과 동일한 조건에서 테스트

## 🔧 CI/CD에서의 테스트

- GitHub Actions에서 PostgreSQL + pgvector 컨테이너 사용
- 모든 테스트는 실제 DB 인스턴스에서 실행
- 테스트 실패 시 빌드 차단

---

**이 정책은 프로젝트의 품질과 신뢰성을 보장하기 위한 필수 규칙입니다.**
**절대 예외를 두지 않습니다.**
