package me.muheun.moaspace.query

import com.pgvector.PGvector
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.SimplePath

// pgvector 연산자 QueryDSL 표현식
object VectorExpression {

    // 코사인 거리 계산 (pgvector <=>)
    fun cosineDistance(
        vectorPath: SimplePath<PGvector>,
        comparisonVector: PGvector
    ): NumberExpression<Double> {
        return Expressions.numberTemplate(
            Double::class.java,
            "{0} <=> {1}",
            vectorPath,
            comparisonVector
        )
    }

    // L2 거리 계산 (pgvector <->)
    fun l2Distance(
        vectorPath: SimplePath<PGvector>,
        comparisonVector: PGvector
    ): NumberExpression<Double> {
        return Expressions.numberTemplate(
            Double::class.java,
            "{0} <-> {1}",
            vectorPath,
            comparisonVector
        )
    }

    // 내적 거리 계산 (pgvector <#>)
    fun innerProductDistance(
        vectorPath: SimplePath<PGvector>,
        comparisonVector: PGvector
    ): NumberExpression<Double> {
        return Expressions.numberTemplate(
            Double::class.java,
            "{0} <#> {1}",
            vectorPath,
            comparisonVector
        )
    }
}
