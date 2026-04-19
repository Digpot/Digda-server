package digdaserver.domain.todo.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.util.UUID

data class TodoUserSummary(
    val userId: UUID,
    val name: String,
    val profileImage: String?
) {
    companion object {
        fun from(user: User): TodoUserSummary = TodoUserSummary(
            userId = user.id,
            name = user.name,
            profileImage = user.profileImage
        )
    }
}
