package me.muheun.moaspace.query

import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicate
import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicates

/**
 * Predicate 유틸리티 함수
 *
 * Kotlin JDSL에서 동적 필터링을 간편하게 처리하기 위한 확장 함수 모음입니다.
 */

/**
 * AND 조건 결합 (nullable 조건 자동 필터링)
 *
 * null이 아닌 조건들만 AND로 결합합니다.
 * 모든 조건이 null인 경우 null을 반환합니다.
 *
 * ### 사용 예시:
 * ```kotlin
 * .where(
 *     whereAnd(
 *         namespace?.let { path(VectorChunk::namespace).eq(it) },
 *         entity?.let { path(VectorChunk::entity).eq(it) },
 *         fieldName?.let { path(VectorChunk::fieldName).eq(it) }
 *     )
 * )
 * ```
 *
 * ### 장점:
 * - nullable 파라미터에 대한 동적 필터링을 간결하게 처리
 * - BooleanBuilder 없이 여러 조건을 AND로 결합
 * - null 체크와 조건 생성을 한 번에 처리
 *
 * @param predicates 결합할 조건들 (nullable)
 * @return null이 아닌 조건들을 AND로 결합한 Predicate, 모든 조건이 null이면 null
 */
fun whereAnd(vararg predicates: Predicate?): Predicate? {
    val nonNullPredicates = predicates.filterNotNull()
    return when {
        nonNullPredicates.isEmpty() -> null
        nonNullPredicates.size == 1 -> nonNullPredicates.first()
        else -> Predicates.and(nonNullPredicates)  // Iterable을 받는 and() 사용
    }
}

/**
 * OR 조건 결합 (nullable 조건 자동 필터링)
 *
 * null이 아닌 조건들만 OR로 결합합니다.
 * 모든 조건이 null인 경우 null을 반환합니다.
 *
 * ### 사용 예시:
 * ```kotlin
 * .where(
 *     whereOr(
 *         title?.let { path(Post::title).like("%${it}%") },
 *         content?.let { path(Post::content).like("%${it}%") }
 *     )
 * )
 * ```
 *
 * @param predicates 결합할 조건들 (nullable)
 * @return null이 아닌 조건들을 OR로 결합한 Predicate, 모든 조건이 null이면 null
 */
fun whereOr(vararg predicates: Predicate?): Predicate? {
    val nonNullPredicates = predicates.filterNotNull()
    return when {
        nonNullPredicates.isEmpty() -> null
        nonNullPredicates.size == 1 -> nonNullPredicates.first()
        else -> Predicates.or(nonNullPredicates)  // Iterable을 받는 or() 사용
    }
}
