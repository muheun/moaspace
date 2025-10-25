package me.muheun.moaspace.service

/**
 * 형태소 분석 및 텍스트 전처리 서비스 인터페이스
 *
 * 한국어 텍스트의 형태소 분석, 정규화, 키워드 추출 등을 담당합니다.
 * 임베딩 전 텍스트 전처리에 사용될 수 있습니다.
 */
interface MorphologyService {

    /**
     * 텍스트를 토큰으로 분리
     *
     * 공백, 구두점 등을 기준으로 텍스트를 토큰 단위로 분리합니다.
     *
     * @param text 토큰화할 텍스트
     * @return List<String> 토큰 목록
     */
    fun tokenize(text: String): List<String>

    /**
     * 텍스트 정규화
     *
     * 공백 제거, 소문자 변환, 특수문자 처리 등을 수행합니다.
     *
     * @param text 정규화할 텍스트
     * @return String 정규화된 텍스트
     */
    fun normalize(text: String): String

    /**
     * 명사 추출
     *
     * 텍스트에서 명사만 추출합니다.
     * 한국어의 경우 형태소 분석을 통해 명사를 추출하고,
     * 영어의 경우 간단한 패턴 매칭을 사용합니다.
     *
     * @param text 명사를 추출할 텍스트
     * @return List<String> 추출된 명사 목록
     */
    fun extractNouns(text: String): List<String>

    /**
     * 키워드 추출 (빈도 기반)
     *
     * 텍스트에서 중요한 키워드를 추출합니다.
     * 빈도 분석을 통해 가장 자주 등장하는 명사를 추출합니다.
     *
     * @param text 키워드를 추출할 텍스트
     * @param topN 추출할 키워드 개수 (기본값: 10)
     * @return List<Pair<String, Int>> 키워드와 빈도 쌍의 목록 (빈도 순 정렬)
     */
    fun extractKeywords(text: String, topN: Int = 10): List<Pair<String, Int>>
}
