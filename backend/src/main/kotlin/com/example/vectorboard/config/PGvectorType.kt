package com.example.vectorboard.config

import com.pgvector.PGvector
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Hibernate UserType for PGvector
 * PGvector 타입을 Hibernate가 올바르게 처리하도록 하는 커스텀 타입
 */
class PGvectorType : UserType<PGvector> {

    override fun getSqlType(): Int = Types.OTHER

    override fun returnedClass(): Class<PGvector> = PGvector::class.java

    override fun equals(x: PGvector?, y: PGvector?): Boolean {
        return x == y
    }

    override fun hashCode(x: PGvector?): Int {
        return x?.hashCode() ?: 0
    }

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor?,
        owner: Any?
    ): PGvector? {
        val value = rs.getObject(position)
        return when (value) {
            null -> null
            is org.postgresql.util.PGobject -> PGvector(value.value)
            else -> null
        }
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: PGvector?,
        index: Int,
        session: SharedSessionContractImplementor?
    ) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            val pgObject = org.postgresql.util.PGobject()
            pgObject.type = "vector"
            pgObject.value = value.toString()
            st.setObject(index, pgObject)
        }
    }

    override fun deepCopy(value: PGvector?): PGvector? {
        return value?.let { PGvector(it.toString()) }
    }

    override fun isMutable(): Boolean = false

    override fun disassemble(value: PGvector?): Serializable? {
        return value?.toString()
    }

    override fun assemble(cached: Serializable?, owner: Any?): PGvector? {
        return cached?.let { PGvector(it.toString()) }
    }
}
