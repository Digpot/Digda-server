package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.util.UUID

data class DiaryUserSummary(
    val userId: UUID,
    val name: String,
    val profileImage: String?
) {
    companion object {
        fun from(user: User): DiaryUserSummary = DiaryUserSummary(
            userId = user.id,
            name = user.displayedName(),
            profileImage = user.profileImage
        )
    }
}
