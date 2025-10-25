package me.muheun.moaspace.dto

/**
 * 벡터 인덱싱 요청 DTO
 *
 * 하나의 레코드에서 여러 필드를 일괄 벡터화할 수 있습니다.
 *
 * @property namespace 네임스페이스 (기본값: "vector_ai")
 * @property entity 엔티티 타입 (예: "posts", "products", "comments")
 * @property recordKey 레코드 식별자 (원본 테이블의 ID)
 * @property fields 필드명과 텍스트 맵 (예: {"title": "제목", "content": "본문"})
 * @property metadata 추가 메타데이터 (JSONB 형식, 예: {"author": "user123", "category": "tech"})
 */
data class VectorIndexRequest(
    val namespace: String = "vector_ai",
    val entity: String,
    val recordKey: String,
    val fields: Map<String, String>,
    val metadata: Map<String, Any>? = null
) {
    init {
        require(entity.isNotBlank()) { "entity는 비어있을 수 없습니다" }
        require(recordKey.isNotBlank()) { "recordKey는 비어있을 수 없습니다" }
        require(fields.isNotEmpty()) { "최소 1개 이상의 필드가 필요합니다" }
    }

    /**
     * 단일 필드 인덱싱을 위한 편의 생성자
     */
    constructor(
        namespace: String = "vector_ai",
        entity: String,
        recordKey: String,
        fieldName: String,
        text: String,
        metadata: Map<String, Any>? = null
    ) : this(
        namespace = namespace,
        entity = entity,
        recordKey = recordKey,
        fields = mapOf(fieldName to text),
        metadata = metadata
    )
}
