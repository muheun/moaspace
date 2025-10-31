package me.muheun.moaspace.event

import org.springframework.context.ApplicationEvent

// 벡터 인덱싱 요청 이벤트
class VectorIndexingRequestedEvent(
    val namespace: String,
    val entity: String,
    val recordKey: String,
    val fields: Map<String, String>,
    val metadata: Map<String, Any>? = null
) : ApplicationEvent("UniversalVectorIndexingService")
