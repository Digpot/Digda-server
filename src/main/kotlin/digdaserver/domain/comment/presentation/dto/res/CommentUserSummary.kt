package digdaserver.domain.comment.presentation.dto.res

import digdaserver.domain.user.domain.entity.User
import java.util.UUID

data class CommentUserSummary(
    val userId: UUID,
    val name: String,
    val profileImage: String?
) {
    companion object {
        fun from(user: User): CommentUserSummary = CommentUserSummary(
            userId = user.id,
            name = user.displayedName(),
            profileImage = user.profileImage
        )
    }
}
