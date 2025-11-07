package me.muheun.moaspace.dto

import me.muheun.moaspace.domain.user.User
import java.time.LocalDateTime

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
