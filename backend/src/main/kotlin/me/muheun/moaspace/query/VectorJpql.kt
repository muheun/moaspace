package me.muheun.moaspace.query

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.JpqlDsl
import com.linecorp.kotlinjdsl.querymodel.jpql.expression.Expression
import com.linecorp.kotlinjdsl.querymodel.jpql.expression.Expressionable
import com.linecorp.kotlinjdsl.querymodel.jpql.expression.Expressions

/**
 * pgvector Custom DSL
 *
 * PostgreSQL pgvector extension의 벡터 연산을 Kotlin JDSL에서 타입 안전하게 사용하기 위한 Custom DSL입니다.
 *
 * ### 주요 연산자:
 * - `cosineDistance()`: pgvector의 <=> (코사인 거리) 연산자를 래핑
 * - `cosineSimilarity()`: 1 - 코사인 거리로 유사도 계산
 *
 * ### 사용 예시:
 * ```kotlin
 * jpqlExecutor.findAll(VectorJpql) {
 *     select(
 *         path(VectorChunk::recordKey),
 *         cosineSimilarity(path(VectorChunk::chunkVector), queryVector)
 *     )
 *     .from(entity(VectorChunk::class))
 *     .orderBy(cosineSimilarity(...).desc())
 * }
 * ```
 */
class VectorJpql : Jpql() {
    companion object Constructor : JpqlDsl.Constructor<VectorJpql> {
        override fun newInstance(): VectorJpql = VectorJpql()
    }

    /**
     * pgvector 코사인 거리 연산 (embedding <=> vector)
     *
     * PostgreSQL pgvector의 <=> 연산자를 사용하여 두 벡터 간 코사인 거리를 계산합니다.
     *
     * @param vector1 비교할 첫 번째 벡터 (Entity 필드 Path)
     * @param vector2 비교할 두 번째 벡터 (FloatArray 쿼리 벡터)
     * @return 코사인 거리 (0~2 범위, 낮을수록 유사)
     */
    fun cosineDistance(
        vector1: Expressionable<*>,
        vector2: FloatArray
    ): Expression<Double> {
        // pgvector의 <=> 연산자를 Native SQL 표현식으로 래핑
        // CAST는 FloatArray를 PostgreSQL vector 타입으로 변환하기 위해 필요
        return customExpression(
            Double::class,
            "({0} <=> CAST({1} AS vector))",
            vector1.toExpression(),
            value(vector2)
        )
    }

    /**
     * 코사인 유사도 계산 (1 - 거리)
     *
     * 코사인 거리를 유사도로 변환합니다. 값이 클수록 유사합니다.
     *
     * @param vector1 비교할 첫 번째 벡터 (Entity 필드 Path)
     * @param vector2 비교할 두 번째 벡터 (FloatArray 쿼리 벡터)
     * @return 코사인 유사도 (-1~1 범위, 1에 가까울수록 유사)
     */
    fun cosineSimilarity(
        vector1: Expressionable<*>,
        vector2: FloatArray
    ): Expression<Double> {
        // 1 - 코사인 거리 = 코사인 유사도
        return Expressions.minus(
            Expressions.value(1.0),
            cosineDistance(vector1, vector2)
        )
    }
}
