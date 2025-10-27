package me.muheun.moaspace.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 벡터 메타데이터 타입 안전 래퍼
 *
 * VectorChunk의 metadata 필드에 저장되는 다양한 메타데이터 타입을 타입 안전하게 처리합니다.
 * Sealed class를 사용하여 컴파일 타임에 타입 검증을 수행하고, 런타임 캐스팅 오류를 방지합니다.
 *
 * **사용 예시**:
 * ```kotlin
 * // 게시글 메타데이터
 * val postMeta = VectorMetadata.PostMetadata(
 *     category = "Technology",
 *     tags = listOf("AI", "ML", "Kotlin"),
 *     author = "user123"
 * )
 *
 * // Map으로 변환 (DB 저장용)
 * val metadataMap = postMeta.toMap()
 *
 * // Map에서 복원
 * val restored = metadataMap.toPostMetadata()
 * ```
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = VectorMetadata.PostMetadata::class, name = "post"),
    JsonSubTypes.Type(value = VectorMetadata.ProductMetadata::class, name = "product"),
    JsonSubTypes.Type(value = VectorMetadata.CommentMetadata::class, name = "comment"),
    JsonSubTypes.Type(value = VectorMetadata.GenericMetadata::class, name = "generic")
)
sealed class VectorMetadata {
    /**
     * 메타데이터를 Map으로 변환합니다 (DB JSONB 저장용)
     */
    abstract fun toMap(): Map<String, Any>

    /**
     * 게시글(Post) 메타데이터
     *
     * @property category 카테고리 (예: "Technology", "Lifestyle")
     * @property tags 태그 목록 (예: ["AI", "ML", "Kotlin"])
     * @property author 작성자 식별자 (예: "user123")
     * @property viewCount 조회수 (검색 랭킹용)
     * @property likeCount 좋아요 수 (검색 랭킹용)
     */
    data class PostMetadata(
        val category: String? = null,
        val tags: List<String>? = null,
        val author: String? = null,
        val viewCount: Int? = null,
        val likeCount: Int? = null
    ) : VectorMetadata() {
        override fun toMap(): Map<String, Any> = buildMap {
            put("type", "post")
            category?.let { put("category", it) }
            tags?.let { put("tags", it) }
            author?.let { put("author", it) }
            viewCount?.let { put("viewCount", it) }
            likeCount?.let { put("likeCount", it) }
        }
    }

    /**
     * 상품(Product) 메타데이터
     *
     * @property category 상품 카테고리 (예: "Electronics", "Fashion")
     * @property brand 브랜드명 (예: "Apple", "Samsung")
     * @property price 가격 (검색 필터링용)
     * @property rating 평점 (1.0 ~ 5.0, 검색 랭킹용)
     * @property stockStatus 재고 상태 ("in_stock", "out_of_stock")
     */
    data class ProductMetadata(
        val category: String? = null,
        val brand: String? = null,
        val price: Double? = null,
        val rating: Double? = null,
        val stockStatus: String? = null
    ) : VectorMetadata() {
        override fun toMap(): Map<String, Any> = buildMap {
            put("type", "product")
            category?.let { put("category", it) }
            brand?.let { put("brand", it) }
            price?.let { put("price", it) }
            rating?.let { put("rating", it) }
            stockStatus?.let { put("stockStatus", it) }
        }
    }

    /**
     * 댓글(Comment) 메타데이터
     *
     * @property parentId 부모 댓글 ID (대댓글인 경우)
     * @property author 작성자 식별자
     * @property likeCount 좋아요 수
     * @property isAccepted 채택 여부 (Q&A 시스템)
     */
    data class CommentMetadata(
        val parentId: String? = null,
        val author: String? = null,
        val likeCount: Int? = null,
        val isAccepted: Boolean? = null
    ) : VectorMetadata() {
        override fun toMap(): Map<String, Any> = buildMap {
            put("type", "comment")
            parentId?.let { put("parentId", it) }
            author?.let { put("author", it) }
            likeCount?.let { put("likeCount", it) }
            isAccepted?.let { put("isAccepted", it) }
        }
    }

    /**
     * 일반 메타데이터 (기타 엔티티용)
     *
     * 특정 타입이 정의되지 않은 엔티티의 메타데이터를 저장합니다.
     * 유연성을 위해 Map<String, Any> 형태로 저장합니다.
     *
     * @property attributes 임의의 key-value 속성 맵
     */
    data class GenericMetadata(
        val attributes: Map<String, Any> = emptyMap()
    ) : VectorMetadata() {
        override fun toMap(): Map<String, Any> = buildMap {
            put("type", "generic")
            putAll(attributes)
        }
    }
}

// ========================================
// 확장 함수: Map ↔ VectorMetadata 변환
// ========================================

/**
 * Map을 PostMetadata로 변환합니다
 *
 * @return PostMetadata 객체
 * @throws IllegalArgumentException type이 "post"가 아닌 경우
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toPostMetadata(): VectorMetadata.PostMetadata {
    require(this["type"] == "post") { "Map의 type이 'post'가 아닙니다: ${this["type"]}" }
    return VectorMetadata.PostMetadata(
        category = this["category"] as? String,
        tags = this["tags"] as? List<String>,
        author = this["author"] as? String,
        viewCount = (this["viewCount"] as? Number)?.toInt(),
        likeCount = (this["likeCount"] as? Number)?.toInt()
    )
}

/**
 * Map을 ProductMetadata로 변환합니다
 *
 * @return ProductMetadata 객체
 * @throws IllegalArgumentException type이 "product"가 아닌 경우
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toProductMetadata(): VectorMetadata.ProductMetadata {
    require(this["type"] == "product") { "Map의 type이 'product'가 아닙니다: ${this["type"]}" }
    return VectorMetadata.ProductMetadata(
        category = this["category"] as? String,
        brand = this["brand"] as? String,
        price = (this["price"] as? Number)?.toDouble(),
        rating = (this["rating"] as? Number)?.toDouble(),
        stockStatus = this["stockStatus"] as? String
    )
}

/**
 * Map을 CommentMetadata로 변환합니다
 *
 * @return CommentMetadata 객체
 * @throws IllegalArgumentException type이 "comment"가 아닌 경우
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toCommentMetadata(): VectorMetadata.CommentMetadata {
    require(this["type"] == "comment") { "Map의 type이 'comment'가 아닌 경우: ${this["type"]}" }
    return VectorMetadata.CommentMetadata(
        parentId = this["parentId"] as? String,
        author = this["author"] as? String,
        likeCount = (this["likeCount"] as? Number)?.toInt(),
        isAccepted = this["isAccepted"] as? Boolean
    )
}

/**
 * Map을 GenericMetadata로 변환합니다
 *
 * @return GenericMetadata 객체
 */
@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toGenericMetadata(): VectorMetadata.GenericMetadata {
    val attributes = this.filterKeys { it != "type" }
    return VectorMetadata.GenericMetadata(attributes)
}

/**
 * Map을 적절한 VectorMetadata 타입으로 자동 변환합니다
 *
 * type 필드를 기준으로 적절한 sealed class 타입으로 변환합니다.
 * type 필드가 없거나 알 수 없는 타입인 경우 GenericMetadata로 변환합니다.
 *
 * @return 변환된 VectorMetadata 객체
 */
fun Map<String, Any>.toVectorMetadata(): VectorMetadata {
    return when (this["type"]) {
        "post" -> toPostMetadata()
        "product" -> toProductMetadata()
        "comment" -> toCommentMetadata()
        else -> toGenericMetadata()
    }
}
