package me.muheun.moaspace.domain.vector

import me.muheun.moaspace.domain.post.Post
import me.muheun.moaspace.domain.user.User
import kotlin.reflect.KClass

/**
 * 벡터 인덱싱 가능한 엔티티 타입 정의
 *
 * 타입 안전성 보장:
 * - entity_type 문자열 하드코딩 방지
 * - 컴파일타임 오타 검증 (IDE 자동완성 지원)
 * - 엔티티 클래스명 변경 시 한 곳만 수정
 *
 * 사용법:
 * ```
 * val entityType = VectorEntityType.POST
 * vectorIndexingService.indexEntity(
 *     entityType = entityType.typeName,  // "Post"
 *     ...
 * )
 * ```
 */
enum class VectorEntityType(val entityClass: KClass<*>) {
    /**
     * 게시글 엔티티
     */
    POST(Post::class),

    /**
     * 사용자 엔티티
     */
    USER(User::class);

    /**
     * 엔티티 클래스의 단순 이름
     * 예: Post::class → "Post"
     */
    val typeName: String
        get() = entityClass.simpleName
            ?: throw IllegalStateException("엔티티 클래스명을 가져올 수 없습니다: ${entityClass}")

    companion object {
        /**
         * 엔티티 인스턴스로부터 VectorEntityType 추론
         *
         * @param entity 벡터 인덱싱 대상 엔티티 인스턴스
         * @return 해당하는 VectorEntityType
         * @throws IllegalArgumentException 지원하지 않는 엔티티 타입
         */
        fun from(entity: Any): VectorEntityType {
            return entries.find { it.entityClass == entity::class }
                ?: throw IllegalArgumentException(
                    "지원하지 않는 엔티티 타입입니다: ${entity::class.simpleName}"
                )
        }

        /**
         * 타입명 문자열로부터 VectorEntityType 조회
         *
         * @param typeName 엔티티 타입명 (예: "Post")
         * @return 해당하는 VectorEntityType
         * @throws IllegalArgumentException 존재하지 않는 타입명
         */
        fun fromTypeName(typeName: String): VectorEntityType {
            return entries.find { it.typeName == typeName }
                ?: throw IllegalArgumentException(
                    "존재하지 않는 엔티티 타입명입니다: $typeName"
                )
        }
    }
}
