package digdaserver.domain.schedule.presentation.dto.res

import digdaserver.domain.comment.domain.entity.Comment
import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val text: String,
    val createdBy: UserSummary,
    /** 아주 오래된 댓글은 작성일시가 NULL 일 수 있어 nullable. 앱은 null 이면 날짜를 숨긴다. */
    val createdAt: LocalDateTime?,
    /** 차단/신고로 숨겨진 댓글인지. true 면 text 는 비워진 상태. */
    val hidden: Boolean = false,
    val hiddenReason: String? = null
) {
    fun asHidden(reason: String): CommentResponse = copy(
        text = "",
        hidden = true,
        hiddenReason = reason
    )

    companion object {
        fun from(comment: Comment): CommentResponse = CommentResponse(
            id = comment.id,
            text = comment.text,
            createdBy = UserSummary.from(comment.createdBy),
            createdAt = comment.createdAt
        )
    }
}
