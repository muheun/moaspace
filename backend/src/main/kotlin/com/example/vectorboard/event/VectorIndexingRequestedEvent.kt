package com.example.vectorboard.event

import org.springframework.context.ApplicationEvent

/**
 * 벡터 인덱싱 요청 이벤트
 *
 * 엔티티의 벡터 인덱싱이 요청되었을 때 발행되는 이벤트입니다.
 * 이벤트 리스너(VectorProcessingService)가 백그라운드에서 비동기로 처리합니다.
 *
 * @property namespace 네임스페이스 (예: "vector_ai")
 * @property entity 엔티티 타입 (예: "posts", "products")
 * @property recordKey 레코드 식별자 (예: "123")
 * @property fields 벡터화할 필드 맵 (fieldName -> fieldValue)
 * @property metadata 추가 메타데이터 (선택사항)
 */
class VectorIndexingRequestedEvent(
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val fields: Map<String, String>,
    val metadata: Map<String, Any>? = null
) : ApplicationEvent("UniversalVectorIndexingService")
