package digdaserver.domain.comment.presentation.dto.res

import digdaserver.domain.comment.domain.entity.Comment
import java.time.LocalDateTime

data class CreateCommentResponse(
    val id: Long,
    val text: String,
    val createdBy: CommentUserSummary,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment): CreateCommentResponse = CreateCommentResponse(
            id = comment.id,
            text = comment.text,
            createdBy = CommentUserSummary.from(comment.createdBy),
            createdAt = comment.createdAt
        )
    }
}
