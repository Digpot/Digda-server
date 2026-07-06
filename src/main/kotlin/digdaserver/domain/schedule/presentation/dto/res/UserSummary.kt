package digdaserver.domain.schedule.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.util.UUID

data class UserSummary(
    val userId: UUID,
    val name: String,
    val profileImage: String?
) {
    companion object {
        fun from(user: User): UserSummary = UserSummary(
            userId = user.id,
            name = user.displayedName(),
            profileImage = user.profileImage
        )
    }
}
