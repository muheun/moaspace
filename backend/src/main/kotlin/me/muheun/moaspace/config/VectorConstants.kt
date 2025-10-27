package me.muheun.moaspace.config

/**
 * 벡터 임베딩 관련 상수 정의
 *
 * 벡터 검색 시스템에서 사용하는 핵심 상수들을 중앙에서 관리합니다.
 * 임베딩 모델 변경 시 이 파일만 수정하면 전체 시스템에 반영됩니다.
 *
 * **현재 모델**: intfloat/multilingual-e5-base
 * **벡터 차원**: 768차원
 *
 * ## 변경 히스토리
 *
 * - **2025-10-27**: multilingual-e5-base (768차원) 적용
 *   - 이전: 1536차원 (Mock Vector)
 *   - 변경 이유: 실제 임베딩 모델 통합, 다국어 지원 강화
 *
 * - **향후 고려 사항**:
 *   - GPU 가속 활성화 시 배치 크기 조정 가능
 *   - 다중 모델 지원 시 모델별 차원 상수 추가
 */
object VectorConstants {

    /**
     * 벡터 임베딩 차원
     *
     * multilingual-e5-base 모델의 고정 출력 차원입니다.
     * 이 값은 다음 위치에서 일치해야 합니다:
     * - DB 스키마: `vector(768)` 컬럼 정의
     * - 도메인 모델: `VectorChunk.chunkVector`
     * - ONNX 모델 출력: 768차원 FloatArray
     *
     * **주의**: 이 값을 변경하면 DB 마이그레이션 필요
     */
    const val VECTOR_DIMENSION = 768

    /**
     * 최대 토큰 길이
     *
     * multilingual-e5-base 모델의 최대 입력 토큰 수입니다.
     * 512 토큰을 초과하는 입력은 자동으로 truncate됩니다.
     *
     * **참고**: 평균 한글 1글자 ≈ 1.5 토큰 (약 340자 = 512토큰)
     */
    const val MAX_TOKEN_LENGTH = 512

    /**
     * 코사인 유사도 검색 임계값
     *
     * 검색 결과로 간주할 최소 코사인 유사도 값입니다.
     * 0.7 이상이면 의미적으로 관련성이 높다고 판단합니다.
     *
     * **범위**: 0.0 (완전 다름) ~ 1.0 (완전 동일)
     */
    const val SIMILARITY_THRESHOLD = 0.7

    /**
     * 기본 검색 결과 개수
     *
     * 벡터 검색 시 반환할 기본 결과 개수입니다.
     * API 호출 시 limit 파라미터로 재정의 가능합니다.
     */
    const val DEFAULT_TOP_K = 10

    /**
     * 모델 정보 (참조용)
     *
     * 현재 사용 중인 임베딩 모델 정보입니다.
     * 코드에서 직접 사용되지는 않지만, 문서화 목적으로 유지합니다.
     */
    const val MODEL_NAME = "intfloat/multilingual-e5-base"
    const val MODEL_TYPE = "ONNX"
    const val SUPPORTED_LANGUAGES = 100  // 지원 언어 수

}
