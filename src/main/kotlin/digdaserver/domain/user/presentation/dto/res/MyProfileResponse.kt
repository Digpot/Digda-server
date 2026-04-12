package digdaserver.domain.user.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.time.LocalDateTime
import java.util.UUID

data class MyProfileResponse(
    val id: UUID,
    val name: String,
    val email: String?,
    val profileImage: String?,
    val statusMessage: String?,
    val provider: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): MyProfileResponse = MyProfileResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            profileImage = user.profileImage,
            statusMessage = user.statusMessage,
            provider = user.socialProvider.name.lowercase(),
            createdAt = user.createdAt
        )
    }
}
