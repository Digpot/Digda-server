package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.comment.domain.entity.Comment
import java.time.LocalDateTime

data class DiaryCommentResponse(
    val id: Long,
    val text: String,
    val createdBy: DiaryUserSummary,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment): DiaryCommentResponse = DiaryCommentResponse(
            id = comment.id,
            text = comment.text,
            createdBy = DiaryUserSummary.from(comment.createdBy),
            createdAt = comment.createdAt
        )
    }
}
