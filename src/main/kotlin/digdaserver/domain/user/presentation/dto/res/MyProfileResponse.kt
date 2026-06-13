package digdaserver.domain.user.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.time.LocalDateTime

data class MyProfileResponse(
    val id: String,
    val name: String,
    val email: String?,
    val profileImage: String?,
    val provider: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): MyProfileResponse = MyProfileResponse(
            id = user.id.toString(),
            name = user.displayedName(),
            email = user.email,
            profileImage = user.profileImage,
            provider = user.socialProvider.name.lowercase(),
            createdAt = user.createdAt
        )
    }
}
