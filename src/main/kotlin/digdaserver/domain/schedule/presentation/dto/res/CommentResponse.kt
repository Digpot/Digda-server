package digdaserver.domain.schedule.presentation.dto.res

import digdaserver.domain.comment.domain.entity.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val text: String,
    val createdBy: UserSummary,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment): CommentResponse = CommentResponse(
            id = comment.id,
            text = comment.text,
            createdBy = UserSummary.from(comment.createdBy),
            createdAt = comment.createdAt
        )
    }
}
