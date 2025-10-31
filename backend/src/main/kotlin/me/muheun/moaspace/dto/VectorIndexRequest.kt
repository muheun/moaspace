package me.muheun.moaspace.dto

// 벡터 인덱싱 요청
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

    // 단일 필드 생성자
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
