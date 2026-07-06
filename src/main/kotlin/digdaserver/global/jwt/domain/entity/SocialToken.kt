package digdaserver.global.jwt.domain.entity

import digdaserver.domain.oauth2.domain.entity.SocialProvider
import org.springframework.data.redis.core.RedisHash
import java.time.LocalDateTime
@RedisHash(value = "SocialToken")
data class SocialToken(
    val id: String,
    val userId: String,
    val provider: SocialProvider,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: LocalDateTime
) {
    companion object {
        fun generateId(userId: String, provider: SocialProvider): String =
            "$userId:${provider.value}"

        fun of(
            userId: String,
            provider: SocialProvider,
            accessToken: String,
            refreshToken: String,
            expiresIn: LocalDateTime
        ): SocialToken =
            SocialToken(
                id = generateId(userId, provider),
                userId = userId,
                provider = provider,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = expiresIn
            )
    }
}
