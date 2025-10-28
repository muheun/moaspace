package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.user.User
import java.time.LocalDateTime

/**
 * User 응답 DTO
 *
 * Constitution Principle IX: frontend/types/api/user.ts와 수동 동기화 필요
 *
 * Frontend 타입 정의:
 * ```typescript
 * export interface UserResponse {
 *   id: number;
 *   email: string;
 *   name: string;
 *   profileImageUrl: string | null;
 *   createdAt: string; // ISO 8601 format
 * }
 * ```
 */
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * User 엔티티를 UserResponse DTO로 변환
         */
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                name = user.name,
                profileImageUrl = user.profileImageUrl,
                createdAt = user.createdAt
            )
        }
    }
}
