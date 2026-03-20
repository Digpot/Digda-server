package digdaserver.global.jwt.domain.entity

import digdaserver.domain.user.domain.entity.Role
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash

@RedisHash(value = "JsonWebToken", timeToLive = 1209600)
data class JsonWebToken(

    @Id
    val refreshToken: String,

    val providerId: String,

    val email: String,

    val role: Role
) {
    companion object {
        fun of(
            refreshToken: String,
            providerId: String,
            email: String,
            role: Role
        ): JsonWebToken = JsonWebToken(
            refreshToken = refreshToken,
            providerId = providerId,
            email = email,
            role = role
        )
    }
}
