package me.muheun.moaspace.query

import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery

// QueryDSL 유틸리티 확장 함수

// null이 아닌 조건들만 AND로 결합
fun whereAnd(vararg predicates: BooleanExpression?): BooleanExpression? {
    return predicates
        .filterNotNull()
        .takeIf { it.isNotEmpty() }
        ?.reduce { acc, predicate -> acc.and(predicate) }
}

// null이 아닌 조건들만 OR로 결합
fun whereOr(vararg predicates: BooleanExpression?): BooleanExpression? {
    return predicates
        .filterNotNull()
        .takeIf { it.isNotEmpty() }
        ?.reduce { acc, predicate -> acc.or(predicate) }
}

// JPAQuery에 동적 조건 추가
fun <T> JPAQuery<T>.whereIf(predicate: Predicate?): JPAQuery<T> {
    return if (predicate != null) {
        this.where(predicate)
    } else {
        this
    }
}

// 대소문자 구분 없는 LIKE 검색
fun com.querydsl.core.types.dsl.StringPath.ilike(pattern: String): BooleanExpression {
    return com.querydsl.core.types.dsl.Expressions.booleanTemplate(
        "{0} ILIKE {1}",
        this,
        pattern
    )
}
