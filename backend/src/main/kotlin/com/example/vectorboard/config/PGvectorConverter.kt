package com.example.vectorboard.config

import com.pgvector.PGvector
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * PGvector 타입을 PostgreSQL vector 타입으로 변환하는 컨버터
 */
@Converter(autoApply = true)
class PGvectorConverter : AttributeConverter<PGvector?, String?> {

    override fun convertToDatabaseColumn(attribute: PGvector?): String? {
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): PGvector? {
        return dbData?.let { PGvector(it) }
    }
}
